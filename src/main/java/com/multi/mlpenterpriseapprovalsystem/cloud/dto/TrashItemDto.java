package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 휴지통 Dto
 *
 * @author : 송현님
 * @filename : TrashItemDto
 * @since : 2026-01-05 오전 12:14 월요일
 */

@Getter
@AllArgsConstructor
public class TrashItemDto {
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
