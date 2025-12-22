package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 채팅방 초대 reqDto
 *
 * @author : 김승기
 * @filename : ReqChatRoomInviteDto
 * @since : 2025. 12. 19. 금요일
 */

@Getter
@NoArgsConstructor
public class ReqChatRoomInviteDto {
    private List<String> memberIds; // 초대할 empId 리스트
}