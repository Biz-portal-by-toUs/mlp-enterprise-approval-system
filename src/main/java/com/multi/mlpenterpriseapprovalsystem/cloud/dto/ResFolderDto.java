package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 폴더 정보를 클라이언트에게 반환할 때 사용하는 응답 DTO
 *
 * @author : 송현님
 * @filename : ResSharedFolderDto
 * @since : 2025-12-29 오전 10:18 월요일
 */

@Getter
@Builder
public class ResFolderDto {

    private Long folderNo;
    private Long parentId;
    private String folderName;
    private FolderScope scope;
    private Long depNo;
    private String ownerId;
    private String path;
    private LocalDateTime createdAt;
}
