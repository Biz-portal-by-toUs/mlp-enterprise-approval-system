package com.multi.mlpenterpriseapprovalsystem.chat.dto;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoomMember;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.RoomType;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
@AllArgsConstructor
public class ResChatRoomListDto {

    private Long roomNo;
    private String roomName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;

    public static ResChatRoomListDto from(ChatRoom room, String myEmpId) {

        Long unreadCount = room.getMembers().stream()
                .filter(m -> m.getEmployee().getEmpId().equals(myEmpId))
                .findFirst()
                .map(ChatRoomMember::getUnreadCount)
                .orElse(0L);

        // 나 제외 멤버 이름 목록
        List<String> otherNames = room.getMembers().stream()
                .map(ChatRoomMember::getEmployee)
                .filter(emp -> !emp.getEmpId().equals(myEmpId))
                .map(Employee::getEmpName)
                .toList();

        // 그룹방이고 roomName이 있으면 그걸 우선 사용 (사용자 지정 방이름 보호)
        String displayName;
        if (room.getRoomType() == RoomType.GROUP
                && room.getRoomName() != null
                && !room.getRoomName().isBlank()) {
            displayName = room.getRoomName();
        } else {
            displayName = buildDisplayName(otherNames);
        }

        return new ResChatRoomListDto(
                room.getRoomNo(),
                displayName,
                room.getLastMessage(),
                room.getLastMessageAt(),
                unreadCount
        );
    }

    private static String buildDisplayName(List<String> names) {
        if (names == null || names.isEmpty()) return "(알 수 없음)";
        if (names.size() <= 2) return String.join(", ", names);
        return names.get(0) + ", " + names.get(1) + " 외 " + (names.size() - 2) + "명";
    }
}