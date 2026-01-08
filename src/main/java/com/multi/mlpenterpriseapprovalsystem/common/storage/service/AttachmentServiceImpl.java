package com.multi.mlpenterpriseapprovalsystem.common.storage.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.domain.Folder;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import com.multi.mlpenterpriseapprovalsystem.cloud.repository.CloudTrashLogRepository;
import com.multi.mlpenterpriseapprovalsystem.cloud.repository.FolderRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.AttachmentDto;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentFileType;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentStatus;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
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

import java.util.*;

import static com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain.CLOUD;

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
    private final FolderRepository folderRepository;
    private final EmployeeRepository employeeRepository;
    private final CloudTrashLogRepository cloudTrashLogRepository;

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

                // 4-2) 도메인별 정책 분기
                Integer displayOrder;

                if (domain == AttachmentDomain.CLOUD) {
                    // ✅ CLOUD: display_order 안 씀 (DB 체크 1~5 회피)
                    displayOrder = null;
                } else {
                    // ✅ 기존 정책 유지: ACTIVE 5개 제한 + 1~5 슬롯
                    long activeCount = all.stream().filter(Attachment::isActive).count();
                    if (activeCount >= 5) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "첨부파일은 최대 5개까지 가능합니다.");
                    }

                    Set<Integer> usedOrders = new HashSet<>();
                    for (Attachment a : all) {
                        if (a.getDisplayOrder() != null) usedOrders.add(a.getDisplayOrder());
                    }

                    displayOrder = findFirstFreeSlot(usedOrders);
                    if (displayOrder == null) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "사용 가능한 display_order가 없습니다. (소프트삭제로 슬롯이 점유 중일 수 있음)"
                        );
                    }
                }

                // 4-4) ✅ (추가) CLOUD일 때 중복 파일명 자동 suffix 처리
                String finalOriginalName = req.originalName();
                if (domain == AttachmentDomain.CLOUD) {
                    finalOriginalName = makeUniqueOriginalName(
                            comId,
                            domain,
                            req.entityId(),
                            AttachmentStatus.ACTIVE,
                            req.originalName(),
                            null
                    );
                }

                // 4-4) ext 추출(선택)
                String ext = extractExt(finalOriginalName);


                // 4-5) 엔티티 생성 + 저장
                Attachment saved = attachmentRepository.save(
                        Attachment.create(
                                comId,
                                domain,
                                req.entityId(),
                                displayOrder,
                                fileType,
                                finalOriginalName,
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

        Attachment a = attachmentRepository
                .findByAttachmentIdAndComIdAndStatus(attachmentId, user.getComId(), AttachmentStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));

        String requesterId = user.getUsername();

        // ✅ 권한 체크
        if (a.getDomain() == AttachmentDomain.PROV_DOCUMENT) {
            boolean isAdmin = user.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_COM_ADMIN".equals(auth.getAuthority()));
            if (!isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 사내 규정 첨부파일을 삭제할 수 있습니다.");
            }

        } else if (a.getDomain() == CLOUD) {
            // ✅ CLOUD: 폴더 scope에 따라 권한 분기
            Folder folder = folderRepository.findByFolderNoAndComId(a.getEntityId(), user.getComId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더가 없습니다."));

            if (folder.getScope() == FolderScope.DEPT /* 프로젝트 enum에 맞게 */) {
                // 공유함(DEPT): 같은 부서면 삭제 허용
                Long myDepNo = resolveDepNo(user);
                if (!Objects.equals(myDepNo, folder.getDepNo())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "같은 부서만 삭제할 수 있습니다.");
                }
            } else {
                // 개인함(PRVT): 기존 정책 유지(업로더만)
                if (a.getCreatedBy() == null || !a.getCreatedBy().equals(requesterId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 업로드한 첨부파일만 삭제할 수 있습니다.");
                }
            }

        } else {
            // ✅ 그 외 도메인: 기존 정책 유지(업로더만)
            if (a.getCreatedBy() == null || !a.getCreatedBy().equals(requesterId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 업로드한 첨부파일만 삭제할 수 있습니다.");
            }
        }


        if (a.getDomain() == CLOUD) {
            String batchId = UUID.randomUUID().toString();

            int updated = attachmentRepository.softDeleteCloudAttachmentById(
                    user.getComId(), attachmentId, requesterId, batchId
            );

            if (updated == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제 대상이 없거나 이미 삭제되었습니다.");
            }
            // ✅ DELETE 로그 적재 (soft-delete 된 직후)
            cloudTrashLogRepository.insertDeleteLogForAttachment(user.getComId(), attachmentId);


            return attachmentId; // (원하면 batchId도 응답에 같이 내려줄 수 있음)
        }

        // 그 외 도메인: 기존 하드삭제 유지
        deleteObjectFromS3(a.getObjectKey());
        attachmentRepository.delete(a);
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

    @Override
    @Transactional
    public void moveCloudAttachment(CustomUser user, Long attachmentId, Long toFolderNo) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");

        String comId = user.getComId();
        String ownerId = resolveOwnerId(user);
        Long depNo = resolveDepNo(user);

        Folder toFolder = folderRepository.findByFolderNoAndComId(toFolderNo, comId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이동 대상 폴더가 없습니다."));

        if (!canView(toFolder, ownerId, depNo))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이동 대상 폴더 권한이 없습니다.");

        // ✅ ACTIVE만 대상으로 잡는 게 안전
        Attachment att = attachmentRepository
                .findByAttachmentIdAndComIdAndStatus(attachmentId, comId, AttachmentStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다."));

        if (att.getDomain() != CLOUD)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLOUD 파일만 이동 가능합니다.");

        Long fromFolderNo = att.getEntityId();
        Folder fromFolder = folderRepository.findByFolderNoAndComId(fromFolderNo, comId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "원본 폴더가 없습니다."));
        if (!canView(fromFolder, ownerId, depNo))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "원본 폴더 권한이 없습니다.");

        if (fromFolder.getScope() != toFolder.getScope())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공유함과 개인함 간 이동은 불가합니다.");

        // ✅ 목적지 폴더: 잠그고 슬롯 계산
        List<Attachment> destAll = attachmentRepository.findAllByRefForUpdate(comId, CLOUD, toFolderNo);

        long destActive = destAll.stream().filter(Attachment::isActive).count();
        if (destActive >= 5)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "대상 폴더 첨부파일은 최대 5개까지 가능합니다.");

        Set<Integer> used = new HashSet<>();
        for (Attachment a : destAll) {
            if (a.getDisplayOrder() != null) used.add(a.getDisplayOrder());
        }

        Integer newOrder = null;
        for (int i = 1; i <= 5; i++) {
            if (!used.contains(i)) { newOrder = i; break; }
        }
        if (newOrder == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "사용 가능한 displayOrder가 없습니다.");

        // ✅ 이동 + 재배정
        att.moveToEntityId(toFolderNo);
        att.changeDisplayOrder(newOrder);
    }

    /* ===== helpers ===== */

    private boolean canView(Folder f, String ownerId, Long depNo) {
        return switch (f.getScope()) {
            case DEPT -> depNo != null && depNo.equals(f.getDepNo());
            case PRVT -> ownerId != null && ownerId.equals(f.getOwnerId());
        };
    }

    private String resolveOwnerId(CustomUser user) {
        try {
            Object v = user.getClass().getMethod("getEmpId").invoke(user);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) {}
        return user.getUsername();
    }

    private Long resolveDepNo(CustomUser user) {
        String empId = user.getUsername();

        Employee emp = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        if (emp.getDepartment() == null || emp.getDepartment().getDepNo() == null) {
            throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        return emp.getDepartment().getDepNo();
    }

    private int nextDisplayOrderForCloud(List<Attachment> all) {
        int max = 0;
        for (Attachment a : all) {
            Integer o = a.getDisplayOrder();
            if (o != null && o > max) max = o;
        }
        return max + 1;
    }

    @Transactional
    public void renameCloudAttachment(CustomUser user, Long attachmentId, String newOriginalName) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        if (newOriginalName == null || newOriginalName.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일명이 필요합니다.");

        Attachment a = attachmentRepository
                .findByAttachmentIdAndComIdAndStatus(attachmentId, user.getComId(), AttachmentStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다."));

        if (a.getDomain() != CLOUD)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLOUD 파일만 이름 변경 가능합니다.");

        // ✅ 폴더 조회 (파일이 속한 폴더 = entityId)
        Folder folder = folderRepository.findByFolderNoAndComId(a.getEntityId(), user.getComId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더가 없습니다."));

        // ✅ 권한 정책
        if (folder.getScope() == FolderScope.DEPT /* 또는 FolderScope.DEPT / enum명에 맞게 */) {
            // 공유함: 같은 부서면 rename 허용
            Long myDepNo = resolveDepNo(user);
            if (!Objects.equals(myDepNo, folder.getDepNo())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "같은 부서만 이름 변경할 수 있습니다.");
            }
        } else {
            // 개인함: 기존 정책 유지(업로더만)
            String requesterId = user.getUsername();
            if (a.getCreatedBy() == null || !a.getCreatedBy().equals(requesterId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 업로드한 파일만 이름 변경할 수 있습니다.");
            }
        }

        String trimmed = newOriginalName.trim();

        String finalOriginalName = makeUniqueOriginalName(
                user.getComId(),
                CLOUD,
                a.getEntityId(),            // ✅ 폴더번호(현재 파일이 속한 폴더)
                AttachmentStatus.ACTIVE,
                trimmed,
                a.getAttachmentId()         // ✅ 자기 자신 제외
        );

        String ext = extractExt(finalOriginalName);
        a.rename(finalOriginalName, ext);
    }

    private static String sanitizeFileBaseName(String input) {
        String cleaned = String.valueOf(input);
        cleaned = cleaned.replaceAll("[\\\\/:*?\"<>|]", "_");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 80) cleaned = cleaned.substring(0, 80);
        if (cleaned.isBlank()) cleaned = "file";
        return cleaned;
    }

    private String makeUniqueOriginalName(
            String comId,
            AttachmentDomain domain,
            Long entityId,
            AttachmentStatus status,
            String requestedName,
            Long excludeAttachmentId // 업로드면 null, rename이면 자기 attachmentId
    ) {
        String cleaned = sanitizeFileBaseName(Optional.ofNullable(requestedName).orElse("file"));

        String base = cleaned;
        String ext = "";
        int dot = cleaned.lastIndexOf('.');
        if (dot > 0 && dot < cleaned.length() - 1) {
            base = cleaned.substring(0, dot);
            ext = cleaned.substring(dot);
        }

        String first = base + ext;

        boolean exists0 = (excludeAttachmentId == null)
                ? attachmentRepository.existsByComIdAndDomainAndEntityIdAndStatusAndOriginalName(comId, domain, entityId, status, first)
                : attachmentRepository.existsByComIdAndDomainAndEntityIdAndStatusAndOriginalNameAndAttachmentIdNot(comId, domain, entityId, status, first, excludeAttachmentId);

        if (!exists0) return first;

        int i = 1;
        while (true) {
            String candidate = base + "(" + i + ")" + ext;

            boolean exists = (excludeAttachmentId == null)
                    ? attachmentRepository.existsByComIdAndDomainAndEntityIdAndStatusAndOriginalName(comId, domain, entityId, status, candidate)
                    : attachmentRepository.existsByComIdAndDomainAndEntityIdAndStatusAndOriginalNameAndAttachmentIdNot(comId, domain, entityId, status, candidate, excludeAttachmentId);

            if (!exists) return candidate;
            i++;
        }
    }

}
