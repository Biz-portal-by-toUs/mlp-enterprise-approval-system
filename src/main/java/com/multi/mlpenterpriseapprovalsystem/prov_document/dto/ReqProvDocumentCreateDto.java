package com.multi.mlpenterpriseapprovalsystem.prov_document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 규정 등록 메타 reqDto
 *
 * @author : 김승기
 * @filename : ReqProvDocumentCreateDto
 * @since : 2025. 12. 29. 월요일
 */

@Getter
@Setter
public class ReqProvDocumentCreateDto {
    @NotBlank
    private String docTitle;      // 규정 제목

    @NotBlank
    private String description;   // 규정 내용(텍스트)

    // 화면에 없으면 프론트에서 안 보내도 됨 -> 서비스에서 기본값 처리
    private Boolean isPublic;

    // ===== 파일 메타 (presign에 필요) =====
    @NotBlank
    private String originalName;

    @NotBlank
    private String contentType;

    @NotNull
    private Long size;
}