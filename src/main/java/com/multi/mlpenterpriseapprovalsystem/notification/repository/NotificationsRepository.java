package com.multi.mlpenterpriseapprovalsystem.notification.repository;

import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.Notifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 jpa레포지토리
 * 
 * @filename    : NoficationsRepository
 * @author      : 김승기
 * @since       : 2026. 1. 3. 토요일
 */
public interface NotificationsRepository extends JpaRepository<Notifications, Long> {
    @Query("SELECT n FROM Notifications n " +
            "WHERE n.receiver.empId = :empId " +
            "AND (:type IS NULL OR n.notificationType = :type) " +
            "AND (:cursorAt IS NULL OR n.createdAt < :cursorAt OR (n.createdAt = :cursorAt AND n.notiNo < :cursorId)) " +
            "ORDER BY n.createdAt DESC, n.notiNo DESC")
    List<Notifications> findNotificationsByCursor(
            @Param("empId") String empId,
            @Param("type") NotificationType type, // 파라미터 추가
            @Param("cursorAt") LocalDateTime cursorAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    // 미읽음 알림 개수 조회 (헤더용)
    long countByReceiver_EmpIdAndIsReadFalse(String empId);

    List<Notifications> findByReceiver_EmpIdAndIsReadFalse(String empId);

    @Modifying
    @Query("DELETE FROM Notifications n WHERE n.receiver.empId = :empId")
    void deleteAllByEmpId(@Param("empId") String empId);
}