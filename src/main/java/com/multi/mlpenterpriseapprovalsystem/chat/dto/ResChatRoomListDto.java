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
    private int unreadCount;

    public static ResChatRoomListDto from(ChatRoom room, String myEmpId) {

        int unreadCount = room.getMembers().stream()
                .filter(m -> m.getEmployee().getEmpId().equals(myEmpId))
                .findFirst()
                .map(ChatRoomMember::getUnreadCount)
                .orElse(0);

        List<String> otherNames = room.getMembers().stream()
                .map(ChatRoomMember::getEmployee)
                .filter(emp -> !emp.getEmpId().equals(myEmpId))
                .map(Employee::getEmpName)
                .toList();

        String displayName;
        if (room.getRoomType() == RoomType.ONE) {
            // 1:1 채팅방: 나를 제외한 유일한 상대방의 이름을 사용
            displayName = otherNames.isEmpty() ? "알 수 없는 사용자" : otherNames.get(0);
        } else {
            // 그룹 채팅방: 저장된 방 이름이 있으면 사용, 없으면 참여자 이름을 합쳐서 생성
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
                unreadCount
        );
    }

    private static String buildDisplayName(List<String> names) {
        if (names == null || names.isEmpty()) return "(알 수 없음)";
        if (names.size() <= 2) return String.join(", ", names);
        return names.get(0) + ", " + names.get(1) + " 외 " + (names.size() - 2) + "명";
    }
}