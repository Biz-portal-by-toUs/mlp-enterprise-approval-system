package com.multi.mlpenterpriseapprovalsystem.cloud.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 휴지통에서 발생하는 작업(삭제/복구/완전삭제)에 대한 감사(Audit) 로그를 저장하는 엔티티입니다.
 *
 * 범위(scope: 부서/개인), 수행 동작(action)과 시각(actionAt), 수행자(actor),
 * 배치 식별자(batchId) 및 대상 정보(itemType/itemId/itemName)를 기록합니다.
 *
 * @author : 송현님
 * @filename : CloudTrashLog
 * @since : 2026-01-07 오후 2:47 수요일
 */

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cloud_trash_log")
public class CloudTrashLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "com_id", nullable = false, length = 3)
    private String comId;

    @Column(name = "scope", nullable = false, length = 10)
    private String scope; // dept | prvt

    @Column(name = "dep_no")
    private Long depNo;

    @Column(name = "action", nullable = false, length = 10)
    private String action; // DELETE | RESTORE | PURGE

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;

    @Column(name = "actor", length = 50)
    private String actor;

    @Column(name = "batch_id", length = 64)
    private String batchId;

    @Column(name = "item_type", nullable = false, length = 10)
    private String itemType; // FILE | FOLDER

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", length = 255)
    private String itemName;
}
