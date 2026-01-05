package com.multi.mlpenterpriseapprovalsystem.cloud.scheduler;

import com.multi.mlpenterpriseapprovalsystem.cloud.repository.FolderRepository;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : CloudTrashPurgeScheduler
 * @since : 2026-01-05 오전 12:27 월요일
 */

@Component
@RequiredArgsConstructor
public class CloudTrashPurgeScheduler {

    private final FolderRepository folderRepository;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredTrash() {
        folderRepository.purgeExpiredFolders();
        attachmentRepository.purgeExpiredCloudAttachments();
    }
}
