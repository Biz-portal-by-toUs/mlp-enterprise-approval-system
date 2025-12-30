package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.RoomType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅방 resDto
 *
 * @author : 김승기
 * @filename : ResChatRoomDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@AllArgsConstructor
public class ResChatRoomDto {
    private Long roomNo;
    @Setter
    private String roomName;
    private RoomType roomType;
    private List<ResChatRoomMemberDto> members;

    /**
     * ChatRoom 엔티티 → 채팅방 상세 DTO 변환
     */
    public static ResChatRoomDto from(ChatRoom chatRoom) {
        return new ResChatRoomDto(
                chatRoom.getRoomNo(),
                chatRoom.getRoomName(),
                chatRoom.getRoomType(),
                chatRoom.getMembers()
                        .stream()
                        .map(ResChatRoomMemberDto::from)
                        .collect(Collectors.toList())
        );
    }

}
