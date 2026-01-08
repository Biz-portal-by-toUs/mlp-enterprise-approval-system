package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 휴지통 목록 조회 API의 응답 항목 DTO 입니다.

 * 휴지통에 존재하는 대상(폴더/파일)의 공통 정보를 하나의 모델로 표현합니다.
 * type이 FOLDER인 경우: size/contentType은 null
 * type이 FILE인 경우: size/contentType이 채워질 수 있음
 *
 * @author : 송현님
 * @filename : TrashItemDto
 * @since : 2026-01-05 오전 12:14 월요일
 */

@Getter
@AllArgsConstructor
public class ResTrashItemDto {
    private String type;          // "FOLDER" | "FILE"
    private Long id;              // folderNo or attachmentId
    private String name;          // folderName or originalName
    private Long parentFolderNo;  // folder.parentId or attachment.entityId
    private String scope;         // "DEPT" | "PRVT"
    private Long depNo;           // dept면 값, prvt면 null
    private String ownerId;       // prvt면 ownerId, dept면 ownerId(폴더 생성자)
    private String deletedBy;
    private LocalDateTime deletedAt;
    private String deleteBatchId;

    // 파일일 때만 의미있는 것들 (폴더면 null)
    private Long size;
    private String contentType;
}
