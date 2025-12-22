package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 채팅 무한 스크롤 resDto
 *
 * @author : 김승기
 * @filename : ResChatEmployeeCursorDto
 * @since : 2025. 12. 20. 토요일
 */

@Getter
@AllArgsConstructor
public class ResChatEmployeeCursorDto {
    private List<ChatEmployeeItemDto> items;
    private String nextCursor;
    private boolean hasNext;
}