package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoomMember;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.RoomType;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 목록 리스트 조회 resDto
 *
 * @author : 김승기
 * @filename : ResChatRoomMemberDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@Setter
@AllArgsConstructor
public class ResChatRoomListDto {

    private Long roomNo;
    private String roomName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
    private RoomType roomType;
    private int memberCount;

    public static ResChatRoomListDto from(ChatRoom room, String myEmpId) {

        int unreadCount = room.getMembers().stream()
                .filter(m -> m.getEmployee().getEmpId().equals(myEmpId))
                .findFirst()
                .map(ChatRoomMember::getUnreadCount)
                .orElse(0);


        List<ChatRoomMember> activeMembers = room.getMembers().stream()
                .filter(ChatRoomMember::isActive) // ✅ 활성 상태인 멤버만 필터링
                .toList();

        List<String> otherNames = activeMembers.stream()
                .map(ChatRoomMember::getEmployee)
                .filter(emp -> !emp.getEmpId().equals(myEmpId))
                .map(Employee::getEmpName)
                .toList();

        String displayName;
        if (room.getRoomType() == RoomType.ONE) {
            displayName = otherNames.isEmpty() ? "알 수 없는 사용자" : otherNames.get(0);
        } else {
            if (room.getRoomName() != null && !room.getRoomName().isBlank()) {
                displayName = room.getRoomName();
            } else {
                displayName = buildDisplayName(otherNames);
            }
        }

        return new ResChatRoomListDto(
                room.getRoomNo(),
                displayName,
                room.getLastMessage(),
                room.getLastMessageAt(),
                unreadCount,
                room.getRoomType(),
                activeMembers.size()
        );
    }

    private static String buildDisplayName(List<String> names) {
        if (names == null || names.isEmpty()) return "(알 수 없음)";
        if (names.size() <= 2) return String.join(", ", names);
        return names.get(0) + ", " + names.get(1) + " 외 " + (names.size() - 2) + "명";
    }
}