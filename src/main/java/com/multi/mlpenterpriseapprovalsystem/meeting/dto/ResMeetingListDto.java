package com.multi.mlpenterpriseapprovalsystem.meeting.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 회의 목록 조회 list resDto
 *
 * @author : 김승기
 * @filename : ResMeetingListDto
 * @since : 2025. 12. 22. 월요일
 */
@Getter
@Builder
public class ResMeetingListDto {
    private List<ResMeetingSimpleDto> meetings;

    private int page;            // 0부터 시작
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}