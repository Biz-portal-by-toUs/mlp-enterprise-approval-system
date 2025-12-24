package com.multi.mlpenterpriseapprovalsystem.common.storage.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;

/**
 * attachment 저장 인터페이스
 *
 * @author : 권지영
 * @filename : AttachmentService
 * @since : 2025. 12. 24. 수요일
 */
public interface AttachmentService {
    AttachmentServiceImpl.CompleteResponse completeUpload(AttachmentServiceImpl.CompleteRequest req, CustomUser user);

    Long softDelete(Long attachmentId, CustomUser user);
}
