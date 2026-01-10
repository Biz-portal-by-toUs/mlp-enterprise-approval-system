package com.multi.mlpenterpriseapprovalsystem.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SseManager
 * @since : 2026. 1. 3. 토요일
 */
@Component
@Slf4j
public class SseManager {
    // 유저당 여러 탭 연결 지원
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // ✅로그인 감지(new-login)용 (empId -> (deviceId -> emitters))
    private final Map<String, Map<String, CopyOnWriteArrayList<SseEmitter>>> deviceScopedEmitters
            = new ConcurrentHashMap<>();


    public SseEmitter createEmitter(String empId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        userEmitters.computeIfAbsent(empId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(empId, emitter));
        emitter.onTimeout(() -> removeEmitter(empId, emitter));
        emitter.onError(ex -> removeEmitter(empId, emitter));

        return emitter;
    }

    // ✅ 이벤트 이름을 파라미터로 받아 어떤 서비스든 호출 가능하게 함
    public void sendToUser(String empId, String eventName, Object data) {
        List<SseEmitter> emitters = userEmitters.get(empId);
        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                removeEmitter(empId, emitter);
            }
        }
    }

    private void removeEmitter(String empId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = userEmitters.get(empId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) userEmitters.remove(empId);
        }
        try { emitter.complete(); } catch (Exception ignore) {}
    }

    public SseEmitter connect(String empId) {
        SseEmitter emitter = createEmitter(empId);

        // (선택) 연결 확인용 이벤트 한 번 보내기
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("ok", true)));
        } catch (IOException | IllegalStateException e) {
            // 연결 직후에도 브라우저가 바로 끊는 경우가 있어서 안전하게 제거
            removeEmitter(empId, emitter);
        }

        return emitter;
    }

    // =========================
    // ✅로그인 감지(new-login) 전용 흐름
    // =========================

    /** new-login용 SSE 연결: empId + deviceId 로 등록 */
    public SseEmitter connectDeviceScoped(String empId, String deviceId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        deviceScopedEmitters
                .computeIfAbsent(empId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(deviceId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeDeviceScopedEmitter(empId, deviceId, emitter));
        emitter.onTimeout(() -> removeDeviceScopedEmitter(empId, deviceId, emitter));
        emitter.onError(ex -> removeDeviceScopedEmitter(empId, deviceId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("ok", true)));
        } catch (IOException | IllegalStateException e) {
            removeDeviceScopedEmitter(empId, deviceId, emitter);
        }

        return emitter;
    }

    /** new-login 이벤트: 현재 deviceId 제외하고 다른 deviceId에게만 전송 */
    public void sendToOtherDevices(String empId, String excludeDeviceId, String eventName, Object data) {
        Map<String, CopyOnWriteArrayList<SseEmitter>> deviceMap = deviceScopedEmitters.get(empId);
        if (deviceMap == null || deviceMap.isEmpty()) return;

        deviceMap.forEach((deviceId, list) -> {
            if (deviceId.equals(excludeDeviceId)) return;

            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (IOException | IllegalStateException e) {
                    removeDeviceScopedEmitter(empId, deviceId, emitter);
                }
            }
        });
    }

    private void removeDeviceScopedEmitter(String empId, String deviceId, SseEmitter emitter) {
        Map<String, CopyOnWriteArrayList<SseEmitter>> deviceMap = deviceScopedEmitters.get(empId);
        if (deviceMap == null) return;

        CopyOnWriteArrayList<SseEmitter> list = deviceMap.get(deviceId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) deviceMap.remove(deviceId);
        }
        if (deviceMap.isEmpty()) deviceScopedEmitters.remove(empId);

        try { emitter.complete(); } catch (Exception ignore) {}
    }


}