package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메세지 resDto
 *
 * @author : 김승기
 * @filename : ResChatMessage
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@NoArgsConstructor
public class ResChatMessageDto {
    private Long roomNo;
    private String content;
}