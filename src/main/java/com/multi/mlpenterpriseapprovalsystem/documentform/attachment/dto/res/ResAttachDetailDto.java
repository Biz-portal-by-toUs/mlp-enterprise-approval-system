package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.res;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ResAttachDetailDto
 * @since : 2026-01-10 토요일
 */

public record ResAttachDetailDto(
        Long attachNo,
        String title,
        String dscp,
        String uploader,
        Long size,
        String path
) {}
