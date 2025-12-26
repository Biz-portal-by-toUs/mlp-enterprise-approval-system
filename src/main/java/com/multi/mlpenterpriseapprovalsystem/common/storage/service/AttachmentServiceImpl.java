package com.multi.mlpenterpriseapprovalsystem.common.storage.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.AttachmentDto;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentFileType;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentStatus;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * attachment 저장 서비스
 *
 * @author : 권지영
 * @filename : AttachmentServiceImpl
 * @since : 2025. 12. 24. 수요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.env}")
    private String env;

    @Override
    @Transactional
    public AttachmentDto.CompleteResponse completeUpload(AttachmentDto.CompleteRequest req, CustomUser user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");

        // 1) 기본 검증
        if (req.domain() == null || req.domain().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain 값이 필요합니다.");
        if (req.entityId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entityId 값이 필요합니다.");
        if (req.fileType() == null || req.fileType().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileType 값이 필요합니다.");
        if (req.objectKey() == null || req.objectKey().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey 값이 필요합니다.");
        if (req.originalName() == null || req.originalName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "originalName 값이 필요합니다.");
        if (req.contentType() == null || req.contentType().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType 값이 필요합니다.");
        if (req.size() == null || req.size() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size 값이 올바르지 않습니다.");

        // 2) enum 변환
        AttachmentDomain domain = parseDomain(req.domain());
        AttachmentFileType fileType = parseFileType(req.fileType());

        String comId = user.getComId();
        String createdBy = user.getUsername(); // 없으면 user.getId() 등으로 바꿔

        // 3) objectKey 위조 방지(경로 규칙 검증)
        // {env}/{comId}/{domain}/{entityId}/{type}/... 이어야만 허용
        String expectedPrefix = env + "/" + comId + "/"
                + domain.name().toLowerCase() + "/"               // approval
                + domain.name().toLowerCase() + "-" + req.entityId() + "/" // approval-1
                + fileType.name().toLowerCase() + "/";            // image

        log.info("[complete] objectKey={}", req.objectKey());
        log.info("[complete] expectedPrefix={}", expectedPrefix);

        if (!req.objectKey().startsWith(expectedPrefix)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "objectKey 경로가 올바르지 않습니다. expectedPrefix=" + expectedPrefix);
        }

        // 4) 동시성 대비: 같은 (comId, domain, entityId) 범위를 잠그고 슬롯 계산+저장
        //    (동시에 complete 2번 들어오면 display_order 충돌할 수 있음)
        //    -> 유니크 제약으로도 막히지만, 여기서 최대한 예방
        int maxRetry = 3;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                // 4-1) 잠금 걸고 전체 첨부 읽기(삭제 포함) → display_order 유니크 충돌 방지
                List<Attachment> all = attachmentRepository.findAllByRefForUpdate(comId, domain, req.entityId());

                // 4-2) ACTIVE 첨부 5개 제한
                long activeCount = all.stream().filter(Attachment::isActive).count();
                if (activeCount >= 5) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "첨부파일은 최대 5개까지 가능합니다.");
                }

                // 4-3) 사용할 수 있는 display_order(1~5) 찾기
                // ⚠️ 소프트삭제가 display_order를 점유하면 재사용 불가하므로, 전체(all) 기준으로 슬롯을 피해서 배정함
                Set<Integer> usedOrders = new HashSet<>();
                for (Attachment a : all) {
                    if (a.getDisplayOrder() != null) usedOrders.add(a.getDisplayOrder());
                }

                Integer displayOrder = findFirstFreeSlot(usedOrders);
                if (displayOrder == null) {
                    // ACTIVE는 5개 미만인데도 슬롯이 없다는 건, 삭제된 row가 슬롯을 점유 중이라는 뜻
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "사용 가능한 display_order가 없습니다. (소프트삭제로 슬롯이 점유 중일 수 있음)"
                    );
                }

                // 4-4) ext 추출(선택)
                String ext = extractExt(req.originalName());

                // 4-5) 엔티티 생성 + 저장
                Attachment saved = attachmentRepository.save(
                        Attachment.create(
                                comId,
                                domain,
                                req.entityId(),
                                displayOrder,
                                fileType,
                                req.originalName(),
                                req.contentType(),
                                req.size(),
                                ext,
                                req.objectKey(),
                                req.etag(),
                                null,          // checksum_sha256는 지금은 null
                                createdBy
                        )
                );

                return new AttachmentDto.CompleteResponse(saved.getAttachmentId(), saved.getDisplayOrder());

            } catch (DataIntegrityViolationException e) {
                // 동시 업로드 타이밍으로 유니크 충돌이 난 케이스 → 몇 번 재시도
                if (attempt == maxRetry) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "동시 업로드로 인해 충돌이 발생했습니다. 다시 시도해 주세요.");
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "complete 처리 실패");
    }

    private AttachmentDomain parseDomain(String v) {
        try { return AttachmentDomain.valueOf(v.trim().toUpperCase()); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "domain 값이 올바르지 않습니다: " + v); }
    }

    private AttachmentFileType parseFileType(String v) {
        try { return AttachmentFileType.valueOf(v.trim().toUpperCase()); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileType 값이 올바르지 않습니다: " + v); }
    }

    private Integer findFirstFreeSlot(Set<Integer> usedOrders) {
        for (int i = 1; i <= 5; i++) {
            if (!usedOrders.contains(i)) return i;
        }
        return null;
    }

    private String extractExt(String originalName) {
        int idx = originalName.lastIndexOf('.');
        if (idx < 0 || idx == originalName.length() - 1) return null;
        String ext = originalName.substring(idx + 1).trim().toLowerCase();
        return ext.isEmpty() ? null : ext;
    }

    @Transactional
    @Override
    public Long softDelete(Long attachmentId, CustomUser user) {
        // 1) 조회 (테넌트 + ACTIVE만)
        Attachment a = attachmentRepository
                .findByAttachmentIdAndComIdAndStatus(attachmentId, user.getComId(), AttachmentStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));

        // 2) 권한 체크: 업로더만 삭제 가능 (너희 정책에 맞게 subjectId/empNo 매핑)
        String requesterId = user.getUsername(); // 예: emp_no
        if (a.getCreatedBy() == null || !a.getCreatedBy().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 업로드한 첨부파일만 삭제할 수 있습니다.");
        }

        // 3) domain 분기
        if (a.getDomain() == AttachmentDomain.CLOUD) {
            // ✅ CLOUD: soft delete
            a.softDelete(); // status=DELETED
            // save 안 해도 영속 상태면 flush되지만 명시적으로 해도 OK
            // attachmentRepository.save(a);
            return a.getAttachmentId();
        }

        // ✅ 그 외: hard delete (S3 + DB)
        deleteObjectFromS3(a.getObjectKey());  // S3 삭제
        attachmentRepository.delete(a);        // DB row 삭제

        return attachmentId;
    }

    private void deleteObjectFromS3(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (S3Exception e) {
            // S3 권한/버킷/키 문제 등
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "S3 삭제 실패: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}