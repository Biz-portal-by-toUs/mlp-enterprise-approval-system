package com.multi.mlpenterpriseapprovalsystem.cloud.service;

import com.multi.mlpenterpriseapprovalsystem.cloud.repository.CloudTrashLogRepository;
import com.multi.mlpenterpriseapprovalsystem.cloud.repository.FolderRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 클라우드 휴지통(soft-delete 된 데이터) 중 "보관기간이 지난 항목"을
 * 영구 삭제(PURGE, hard delete)하는 서비스
 *
 *  책임(Responsibilities)
 * 1) PURGE 대상에 대한 로그(cloud_trash_log) 적재
 * 2) S3에 저장된 실제 파일 오브젝트 삭제
 * 3) DB에서 attachment / folder 하드 삭제
 *
 * @author : 송현님
 * @filename : CloudTrashPurgeService
 * @since : 2026-01-06 오전 11:35 화요일
 */

@Service
@RequiredArgsConstructor
public class CloudTrashPurgeService {

    private final AttachmentRepository attachmentRepository;
    private final FolderRepository folderRepository;
    private final S3Client s3Client;
    private final CloudTrashLogRepository cloudTrashLogRepository;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Transactional
    public void purgeExpiredTrashDays(int days) {

        // ✅ 0) PURGE 로그 먼저 적재 (하드삭제하면 데이터가 사라짐)
        cloudTrashLogRepository.insertPurgeLogsForExpiredAttachments(days);
        cloudTrashLogRepository.insertPurgeLogsForExpiredFolders(days);

        // 1) S3 삭제할 키 조회 (중복 제거 권장)
        List<String> keys = attachmentRepository.findExpiredDeletedCloudObjectKeys(days);
        keys = keys == null ? List.of() : keys.stream().distinct().toList();

        // 2) S3 먼저 삭제 (부분 실패까지 체크!)
        deleteObjectsFromS3(keys);

        // 3) attachment DB 하드삭제 (만료된 DELETED만)
        attachmentRepository.hardDeleteExpiredDeletedCloudAttachments(days);

        // 4) folder DB 하드삭제
        //    "자식부터"가 필요하면 IN 삭제는 순서 보장 안 됨 → 안전하게 하나씩 삭제
        List<Long> folderNos = folderRepository.findExpiredDeletedFolderNos(days);
        if (folderNos != null && !folderNos.isEmpty()) {
            // ✅ 전사 purge면 comId 없이 삭제하는 메서드가 필요
            folderRepository.hardDeleteExpiredDeletedFolders(days);

            // 만약 FK 때문에 정말 순서가 중요하면, 아래처럼 "1개씩" 지우는 repo 메서드로 바꾸는게 더 안전
            // for (Long id : folderNos) folderRepository.hardDeleteFolderByNo(id);
        }
    }

    private void deleteObjectsFromS3(List<String> keys) {
        if (keys == null || keys.isEmpty()) return;

        for (int i = 0; i < keys.size(); i += 1000) {
            List<String> chunk = keys.subList(i, Math.min(i + 1000, keys.size()));

            var objects = chunk.stream()
                    .map(k -> software.amazon.awssdk.services.s3.model.ObjectIdentifier.builder().key(k).build())
                    .toList();

            try {
                var resp = s3Client.deleteObjects(b -> b
                        .bucket(bucket)
                        .delete(d -> d.objects(objects))
                );

                // ✅ 부분 실패 체크 (중요)
                if (resp != null && resp.hasErrors() && !resp.errors().isEmpty()) {
                    throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
                }

            } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
                throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
            }
        }
    }

}

