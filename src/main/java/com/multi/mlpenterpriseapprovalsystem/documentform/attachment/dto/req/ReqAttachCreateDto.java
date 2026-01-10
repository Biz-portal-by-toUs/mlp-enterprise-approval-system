package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.req;

import jakarta.validation.constraints.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ReqAttachCreate
 * @since : 2026-01-10 토요일
 */

public record ReqAttachCreateDto(
        @NotBlank String title,
        String dscp,
        String path,
        @Positive Long size
) { }
