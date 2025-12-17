package com.multi.mlpenterpriseapprovalsystem.chat.repository;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 채팅방 멤버 엔티티 접근 repository
 *
 * @author : 김승기
 * @filename : ChatRoomMemberRepository
 * @since : 2025. 12. 17. 수요일
 */
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    boolean existsByChatRoom_RoomNoAndEmployee_EmpId(Long roomNo, String empId);
}
