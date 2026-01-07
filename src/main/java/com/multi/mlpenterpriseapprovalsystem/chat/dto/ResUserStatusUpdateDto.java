package com.multi.mlpenterpriseapprovalsystem.chat.dto;

/**
 * 채팅 사원탭에서 status 즉시 업데이트 되게 하는 resDto
 *
 * @author : 김승기
 * @filename : ResUserStatusUpdateDto
 * @since : 2026. 1. 7. 수요일
 */
public record ResUserStatusUpdateDto(
        String empId,
        String msgStat // 'C', 'M', 'D', 'X' 등
) {}