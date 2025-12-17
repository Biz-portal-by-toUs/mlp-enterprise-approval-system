package com.multi.mlpenterpriseapprovalsystem.chat.repository;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 채팅방 엔티티 접근 repository
 *
 * @author : 김승기
 * @filename : ChatRoomRepository
 * @since : 2025. 12. 17. 수요일
 */
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("""
    select cr
    from ChatRoom cr
    join cr.members m
    where cr.roomType = :roomType
      and m.employee.empId in (:empId1, :empId2)
    group by cr
    having count(m) = 2
""")
    Optional<ChatRoom> findOneToOneRoom(
            @Param("roomType") RoomType roomType,
            @Param("empId1") String empId1,
            @Param("empId2") String empId2
    );

    /**
     * 사용자가 속한 채팅방 목록을 마지막 메시지 시간 기준(커서 기반)으로 조회
     */
    @Query("SELECT r FROM ChatRoom r " +
            "JOIN r.members m " +
            "WHERE m.employee.empId = :empId " + // ChatRoomMember 엔티티의 필드 구조에 따라 수정 필요
            "AND (:cursor IS NULL OR r.lastMessageAt < :cursor) " +
            "ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findMyRooms(
            @Param("empId") String empId,
            @Param("cursor") LocalDateTime cursor,
            @Param("size") int size // 참고: size는 @Query 내에서 직접 limit 처리가 안 되므로 Pageable 사용 권장
    );
}
