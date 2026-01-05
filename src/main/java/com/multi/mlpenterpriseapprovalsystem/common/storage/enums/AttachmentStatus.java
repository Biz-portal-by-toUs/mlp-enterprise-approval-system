package com.multi.mlpenterpriseapprovalsystem.common.storage.enums;

/**
 * s3 파일 삭제 여부 enum
 *
 * @author : 권지영
 * @filename : AttachmentStatus
 * @since : 2025. 12. 24. 수요일
 */
public enum AttachmentStatus {
    ACTIVE, DELETED, PURGED
}

// 스케줄러가 S3 삭제까지 끝냈으면 PURGED