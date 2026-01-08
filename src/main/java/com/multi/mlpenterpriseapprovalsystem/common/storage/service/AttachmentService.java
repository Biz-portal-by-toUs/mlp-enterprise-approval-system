package com.multi.mlpenterpriseapprovalsystem.common.storage.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.AttachmentDto;

/**
 * attachment 저장 인터페이스
 *
 * @author : 권지영
 * @filename : AttachmentService
 * @since : 2025. 12. 24. 수요일
 */
public interface AttachmentService {
    AttachmentDto.CompleteResponse completeUpload(AttachmentDto.CompleteRequest req, CustomUser user);

    Long softDelete(Long attachmentId, CustomUser user);

    void moveCloudAttachment(CustomUser user, Long attachmentId, Long toFolderNo);

    void renameCloudAttachment(CustomUser user, Long attachmentId, String newOriginalName);

}
