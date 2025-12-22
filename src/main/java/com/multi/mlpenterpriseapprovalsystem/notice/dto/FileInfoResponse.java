package com.multi.mlpenterpriseapprovalsystem.notice.dto;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : FileinfoDto
 * @since : 2025-12-21 일요일
 */
public record FileInfoResponse(
        String id,
        String originalFilename,
        long size,
        LocalDateTime createdAt
) {}
