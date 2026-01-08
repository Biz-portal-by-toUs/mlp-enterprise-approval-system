package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 /**
 * 관리자 휴지통 감사 로그(삭제/복구/완전삭제) 조회 응답에 사용되는 DTO입니다.
 *
 * 누가(actor) 언제(actionAt) 어떤 동작(action)을 수행했는지와,
 * 해당 동작이 적용된 대상(itemType/itemId/itemName) 및 배치 식별자(batchId)를 포함합니다.

 *
 * @author : 송현님
 * @filename : AdminTrashLogDto
 * @since : 2026-01-07 오후 2:39 수요일
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResAdminTrashLogDto {
    private String action;        // DELETE / RESTORE / PURGE
    private LocalDateTime actionAt;
    private String actor;
    private String batchId;

    private String itemType;      // FILE / FOLDER
    private Long itemId;
    private String itemName;
}
