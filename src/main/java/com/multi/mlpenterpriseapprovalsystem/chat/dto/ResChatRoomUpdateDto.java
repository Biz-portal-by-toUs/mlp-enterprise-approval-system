package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.RoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 업데이트 resDto
 *
 * @author : 김승기
 * @filename : ResChatRoomUpdateDto
 * @since : 2025. 12. 19. 금요일
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResChatRoomUpdateDto {
    private Long roomNo;
    private String lastMessage;
    private String lastMessageAt;
    private int unreadCount;
    private String roomName;
    private boolean removed;
    private RoomType roomType;
    private int memberCount;

}
