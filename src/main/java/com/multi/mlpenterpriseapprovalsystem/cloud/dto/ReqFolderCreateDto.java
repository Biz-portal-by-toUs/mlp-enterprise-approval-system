package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 폴더 생성 요청을 받을 때 사용하는 DTO
 *
 * @author : 송현님
 * @filename : ReqFolderCreateSimpleDto
 * @since : 2025-12-30 오후 3:07 화요일
 */

@Getter
@Setter
public class ReqFolderCreateDto {

    private Long parentId; // null이면 루트

    @NotBlank
    @Size(max = 255)
    private String folderName;
}
