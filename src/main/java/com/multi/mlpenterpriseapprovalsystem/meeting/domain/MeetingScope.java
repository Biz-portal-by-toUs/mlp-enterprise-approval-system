package com.multi.mlpenterpriseapprovalsystem.meeting.domain;

/**
 * 회의 탭 별로 나눈 스코프 (전체회의, 내 부서 회의, 내 회의)
 *
 * @author : 김승기
 * @filename : MeetingScope
 * @since : 2025. 12. 23. 화요일
 */
public enum MeetingScope {
    ALL,        // 전체회의 탭
    MY_DEPT,    // 내 부서 회의 탭
    MY          // 내 회의 탭
}