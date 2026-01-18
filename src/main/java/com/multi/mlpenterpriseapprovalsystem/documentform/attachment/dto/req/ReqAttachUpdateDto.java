package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.req;

import jakarta.validation.constraints.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ReqAttachUpdateDto
 * @since : 2026-01-18 일요일
 */
public record ReqAttachUpdateDto(
        @NotBlank String title,
        String dscp
) {}
