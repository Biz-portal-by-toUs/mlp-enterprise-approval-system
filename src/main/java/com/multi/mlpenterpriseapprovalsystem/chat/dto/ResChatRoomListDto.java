package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoomMember;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 리스트 resDto
 *
 * @author : 김승기
 * @filename : ResChatRoomListDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@AllArgsConstructor
public class ResChatRoomListDto {

    private Long roomNo;
    private String roomName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;

    public static ResChatRoomListDto from(ChatRoom room, String myEmpId) {

        List<String> otherNames = room.getMembers().stream()
                .map(ChatRoomMember::getEmployee)
                .filter(emp -> !emp.getEmpId().equals(myEmpId))
                .map(Employee::getEmpName)
                .toList();

        String displayName = buildDisplayName(otherNames);

        return new ResChatRoomListDto(
                room.getRoomNo(),
                displayName,
                room.getLastMessage(),
                room.getLastMessageAt()
        );
    }

    private static String buildDisplayName(List<String> names) {
        if (names.size() <= 2) {
            return String.join(", ", names);
        }
        return names.get(0) + ", " + names.get(1) + " 외 " + (names.size() - 2) + "명";
    }
}