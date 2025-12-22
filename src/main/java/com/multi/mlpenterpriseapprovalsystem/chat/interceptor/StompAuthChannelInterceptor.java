package com.multi.mlpenterpriseapprovalsystem.chat.interceptor;

import com.multi.mlpenterpriseapprovalsystem.auth.service.EmployeeUserDetailService;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomMemberRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 웹소켓(STOMP) 통신에서 보안(인증 및 권한 부여)을 담당하는 문지기(Interceptor) 역할
 *
 * @author : 김승기
 * @filename : StompAuthChannelInterceptor
 * @since : 2025. 12. 18. 목요일
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final TokenProvider tokenProvider;
    private final EmployeeUserDetailService userDetailsService;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final StringRedisTemplate redisTemplate;

    private static final Pattern ROOM_DEST =
            Pattern.compile("^/sub/chat/rooms/(\\d+)$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (acc == null || acc.getCommand() == null) return message;

        try {
            if (StompCommand.CONNECT.equals(acc.getCommand())) {
                handleConnect(acc);
            } else if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
                handleSubscribe(acc); // 여기서 Redis에 "접속 중" 기록
            } else if (StompCommand.DISCONNECT.equals(acc.getCommand())) {
                handleDisconnect(acc); // 여기서 Redis에서 제거
            }
        } catch (Exception e) {
            log.error("STOMP {} failed. dest={}, user={}",
                    acc.getCommand(), acc.getDestination(), acc.getUser(), e);
            throw e; // 에러 프레임 나가고 끊김(개발 중 원인 찾기엔 좋음)
        }

        // 수정된 헤더를 반영해서 새 메시지로 반환
        return MessageBuilder.createMessage(message.getPayload(), acc.getMessageHeaders());
    }

    private void handleConnect(StompHeaderAccessor acc) {
        String auth = acc.getFirstNativeHeader("Authorization");
        if (auth == null) auth = acc.getFirstNativeHeader("authorization");

        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("No JWT in CONNECT");
        }

        String token = auth.substring(7);
        tokenProvider.validateToken(token);

        String empId = tokenProvider.getUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(empId);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

        acc.setUser(authentication);
    }

    private void handleSubscribe(StompHeaderAccessor acc) {
        String destination = acc.getDestination();
        if (destination == null) return;

        Matcher m = ROOM_DEST.matcher(destination);
        if (m.matches()) {
            Long roomNo = Long.parseLong(m.group(1));
            String empId = extractEmpId(acc);

            boolean isMember = chatRoomMemberRepository.existsByChatRoom_RoomNoAndEmployee_EmpId(roomNo, empId);
            if (!isMember) throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);

            String key = "chat:room:" + roomNo + ":viewing";
            redisTemplate.opsForSet().add(key, empId);

            acc.getSessionAttributes().put("viewingRoomNo", String.valueOf(roomNo));
        }
    }

    private void handleDisconnect(StompHeaderAccessor acc) {
        String empId = extractEmpId(acc);
        String roomNo = (String) acc.getSessionAttributes().get("viewingRoomNo");

        if (roomNo != null) {
            String key = "chat:room:" + roomNo + ":viewing";
            redisTemplate.opsForSet().remove(key, empId);
        }
    }

    private String extractEmpId(StompHeaderAccessor acc) {
        Principal principal = acc.getUser();
        if (principal == null) {
            throw new CustomException(ErrorCode.SOCKET_AUTHENTICATION_ERROR);
        }

        if (principal instanceof Authentication auth) {
            Object p = auth.getPrincipal();
            if (p instanceof UserDetails ud) return ud.getUsername();
            return auth.getName();
        }
        return principal.getName();
    }
}