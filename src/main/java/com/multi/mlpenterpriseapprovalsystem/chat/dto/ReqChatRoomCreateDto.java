package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import lombok.Getter;

import java.util.List;

/**
 * 채팅방 생성 reqDto
 *
 * @author : 김승기
 * @filename : ReqChatRoomCreateDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
public class ReqChatRoomCreateDto {
    private String roomName; // GROUP만 사용
    private List<String> memberIds; // 초대한 사원 ID들
}
