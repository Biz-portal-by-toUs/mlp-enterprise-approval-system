package com.multi.mlpenterpriseapprovalsystem.cloud.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentStatus;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * 파일 관련 비즈니스 로직을 담당하는 서비스입니다.
 *
 * 주요 역할:
 *   파일 저장용 이름 생성: 원본 파일명으로부터 displayName(표시명)과 storedName(저장명)을 생성
 *   ZIP 다운로드 지원: 선택된 fileIds에 대한 파일 메타 조회 및 권한 검증(예정)
 *   스토리지에서 파일 스트리밍: ZIP OutputStream 등 외부 스트림으로 파일 바이트를 복사
 *
 * 컨트롤러는 I/O(요청/응답) 처리만 담당하고,
 * 실제 데이터 조회/권한 체크/스토리지 접근은 본 서비스에서 수행합니다.

 *
 * @author : 송현님
 * @filename : FileService
 * @since : 2026-01-07 오후 5:40 수요일
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final AttachmentRepository attachmentRepository;
    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    /** ZIP 다운로드 대상 Attachment 조회 (CLOUD + ACTIVE + comId 제한) */
    public List<Attachment> getAttachmentsForDownload(CustomUser user, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty())
            throw new IllegalArgumentException("fileIds is required");
        if (user == null) throw new IllegalArgumentException("user is required");

        List<Attachment> found = attachmentRepository.findAllForZip(
                user.getComId(),
                AttachmentDomain.CLOUD,
                AttachmentStatus.ACTIVE,
                attachmentIds
        );

        // 누락 체크
        Set<Long> foundIds = new HashSet<>();
        for (Attachment a : found) foundIds.add(a.getAttachmentId());

        List<Long> missing = new ArrayList<>();
        for (Long id : attachmentIds) {
            if (!foundIds.contains(id)) missing.add(id);
        }
        if (!missing.isEmpty()) throw new NoSuchElementException("Files not found: " + missing);

        // TODO: 추가 권한 체크가 필요하면 여기서(폴더 scope/owner 등)
        return found;
    }

    /** S3(objectKey)에서 파일을 읽어 out(ZipOutputStream 등)으로 스트리밍 복사 */
    public void writeAttachmentToStream(Attachment att, OutputStream out) {
        String objectKey = att.getObjectKey();

        try (InputStream in = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build()
        )) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to stream attachment id=" + att.getAttachmentId(), e);
        }
    }

    // ---------- helpers ----------
    private static String sanitizeFileBaseName(String input) {
        String cleaned = input.replaceAll("[\\\\/:*?\"<>|]", "_");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 80) cleaned = cleaned.substring(0, 80);
        if (!StringUtils.hasText(cleaned)) cleaned = "file";
        return cleaned;
    }

    private String makeUniqueOriginalName(
            String comId,
            AttachmentDomain domain,
            Long entityId,
            AttachmentStatus status,
            String requestedName,
            Long excludeAttachmentId // ✅ null이면 제외 없음(업로드), 값 있으면 rename
    ) {
        String cleaned = sanitizeFileBaseName(Optional.ofNullable(requestedName).orElse("file"));

        String base = cleaned;
        String ext = "";
        int dot = cleaned.lastIndexOf('.');
        if (dot > 0 && dot < cleaned.length() - 1) {
            base = cleaned.substring(0, dot);
            ext = cleaned.substring(dot);
        }

        // exists check (exclude 여부 분기)
        boolean exists0 = (excludeAttachmentId == null)
                ? attachmentRepository.existsByComIdAndDomainAndEntityIdAndStatusAndOriginalName(comId, domain, entityId, status, base + ext)
                : attachmentRepository.existsByComIdAndDomainAndEntityIdAndStatusAndOriginalNameAndAttachmentIdNot(comId, domain, entityId, status, base + ext, excludeAttachmentId);

        if (!exists0) return base + ext;

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
