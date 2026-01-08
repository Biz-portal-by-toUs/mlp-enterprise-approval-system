package com.multi.mlpenterpriseapprovalsystem.cloud.scheduler;

import com.multi.mlpenterpriseapprovalsystem.cloud.repository.FolderRepository;
import com.multi.mlpenterpriseapprovalsystem.cloud.service.CloudTrashPurgeService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클라우드 휴지통(soft-delete 된 데이터) 중 "보관기간이 지난 항목"을
 * 정해진 시간에 자동으로 영구 삭제(PURGE)하기 위한 스케줄러 클래스
 *
 *  역할
 * - 매일 새벽 3시에 purge 작업을 실행
 * - 실제 삭제 로직은 CloudTrashPurgeService에 위임
 *
 * @author : 송현님
 * @filename : CloudTrashPurgeScheduler
 * @since : 2026-01-05 오전 12:27 월요일
 */

@Component
@RequiredArgsConstructor
public class CloudTrashPurgeScheduler {

    private final CloudTrashPurgeService cloudTrashPurgeService;

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredTrash() {
        cloudTrashPurgeService.purgeExpiredTrashDays(30);
    }
}
