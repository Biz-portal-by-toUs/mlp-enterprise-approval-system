package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 폴더 이름 변경 요청을 받을 때 사용하는 DTO
 *
 * @author : 송현님
 * @filename : ReqRenameDto
 * @since : 2025-12-29 오후 5:13 월요일
 */

@Getter
@Setter
public class ReqRenameDto {
    @NotBlank
    @Size(max = 255)
    private String folderName;
}
