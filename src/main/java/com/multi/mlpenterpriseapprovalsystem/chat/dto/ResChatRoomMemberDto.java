package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoomMember;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 채팅방 멤버 resDto
 *
 * @author : 김승기
 * @filename : ResChatRoomMemberDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@AllArgsConstructor
public class ResChatRoomMemberDto {
    private String empId;

    public static ResChatRoomMemberDto from(ChatRoomMember member) {
        return new ResChatRoomMemberDto(
                member.getEmployee().getEmpId()
        );
    }
}
