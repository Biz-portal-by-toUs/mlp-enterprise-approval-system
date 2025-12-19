package com.multi.mlpenterpriseapprovalsystem.chat.repository;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 채팅방 멤버 엔티티 접근 repository
 *
 * @author : 김승기
 * @filename : ChatRoomMemberRepository
 * @since : 2025. 12. 17. 수요일
 */
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    boolean existsByChatRoom_RoomNoAndEmployee_EmpId(Long roomNo, String empId);

    Optional<ChatRoomMember> findByChatRoom_RoomNoAndEmployee_EmpId(Long roomNo, String empId);

    @Query("select m.employee.empId from ChatRoomMember m where m.chatRoom.roomNo = :roomNo")
    List<String> findEmpIdsByRoomNo(@Param("roomNo") Long roomNo);

    @Query("select m.unreadCount from ChatRoomMember m where m.chatRoom.roomNo = :roomNo and m.employee.empId = :empId")
    int findUnreadCount(@Param("roomNo") Long roomNo, @Param("empId") String empId);

    @Modifying
    @Query("""
        update ChatRoomMember m
           set m.unreadCount = m.unreadCount + 1
         where m.chatRoom.roomNo = :roomNo
           and m.employee.empId <> :senderEmpId
    """)
    int increaseUnreadForOthers(@Param("roomNo") Long roomNo,
                                @Param("senderEmpId") String senderEmpId);
}
