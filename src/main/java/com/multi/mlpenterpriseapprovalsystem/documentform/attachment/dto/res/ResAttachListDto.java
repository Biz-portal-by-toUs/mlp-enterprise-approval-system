package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.res;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ResAttachList
 * @since : 2026-01-10 토요일
 */

public record ResAttachListDto(
        Long attachNo,
        String title,
        String uploader,
        Long size
) {}