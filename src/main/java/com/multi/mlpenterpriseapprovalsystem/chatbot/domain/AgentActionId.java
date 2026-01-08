package com.multi.mlpenterpriseapprovalsystem.chatbot.domain;

import java.util.Set;

/**
 * 챗봇 에이전트 액션
 *
 * @author : 김승기
 * @filename : AgentActionId
 * @since : 2026. 1. 6. 화요일
 */
public enum AgentActionId {

    // ===== Mail =====
    NAV_MAIL_COMPOSE("/mail/write", Set.of(), "메일 작성 화면으로 이동"),

    // ===== Reservations (내 예약 조회) =====
    NAV_MY_RESERVATIONS("/my-reservations", Set.of(), "내 예약 조회로 이동"),

    // ===== Schedule =====
    NAV_TODAY_SCHEDULE("/schedule/calendar", Set.of(), "오늘 일정 페이지로 이동"),

    // ===== Approval =====
    NAV_APPROVAL_DRAFT("/document-forms",
            Set.of(),
            "결재 작성 페이지로 이동");


    private final String urlTemplate;
    private final Set<String> requiredParams;
    private final String defaultLabel;

    AgentActionId(String urlTemplate, Set<String> requiredParams, String defaultLabel) {
        this.urlTemplate = urlTemplate;
        this.requiredParams = requiredParams;
        this.defaultLabel = defaultLabel;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public Set<String> getRequiredParams() {
        return requiredParams;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }
}