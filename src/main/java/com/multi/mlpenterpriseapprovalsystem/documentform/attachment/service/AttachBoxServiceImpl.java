package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.domain.AttachBox;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.res.ResAttachDelDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.res.ResAttachDetailDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.res.ResAttachListDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.repository.AttachBoxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : AttachBoxServiceImpl
 * @since : 2026-01-10 토요일
 */

@Service
@RequiredArgsConstructor
public class AttachBoxServiceImpl implements AttachBoxService {

    private final AttachBoxRepository attachBoxRepository;
    private final CompanyRepository companyRepository;

    // Storage(Attachment) 연계: S3 + attachment 테이블 하드삭제
    private final AttachmentRepository attachmentRepository;
    private final S3Client s3Client;

    // 테스트/로컬에서 설정 누락 시 빈 문자열로 들어오게 해서 원인 파악 쉽게
    @Value("${app.s3.bucket:}")
    private String bucket;

    @Override
    @Transactional
    public Long create(ReqAttachCreateDto req, CustomUser user) {
        String comId = requireComId(user);
        String uploader = requireUploader(user);

        if (req.title() == null || req.title().isEmpty())
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TITLE_REQUIRED);

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        AttachBox saved = attachBoxRepository.save(
                AttachBox.create(
                        company,
                        uploader,
                        req.title(),
                        req.dscp(),
                        req.path(),
                        req.size()
                )
        );

        return saved.getAttachNo();
    }

    @Override
    public Page<ResAttachListDto> list(Pageable pageable, CustomUser user) {
        String comId = requireComId(user);

        return attachBoxRepository
                .findByCompany_ComIdAndCommittedTrueOrderByAttachNoDesc(comId, pageable)
                .map(a -> new ResAttachListDto(
                        a.getAttachNo(),
                        a.getTitle(),
                        a.getUploader(),
                        a.getSize()
                ));
    }

    @Override
    public ResAttachDetailDto detail(Long attachNo, CustomUser user) {
        String comId = requireComId(user);

        AttachBox a = attachBoxRepository
                .findByAttachNoAndCompany_ComId(attachNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));

        return toDetail(a);
    }

    /* 하드 딜리트 (AttachBox 기준)
     *
     * 순서:
     * 1) AttachBox 존재 확인
     * 2) attachment(objectKey) 조회
     * 3) S3 삭제 (배치 1000개)
     * 4) attachment DB 하드삭제
     * 5) attach_box DB 하드삭제
     */
    @Override
    @Transactional
    public ResAttachDelDto delete(Long attachNo, CustomUser user) {
        String comId = requireComId(user);

        // 1) AttachBox 존재 확인
        attachBoxRepository.findByAttachNoAndCompany_ComId(attachNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));

        // 2) S3 bucket 설정 검증
        if (bucket == null || bucket.isBlank()) {
            // 설정 누락이면 "왜 안 지워지지?"가 아니라 바로 원인 보이게
            throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
        }

        // 3) S3 삭제할 키 조회 (Attachment.domain=DOC_FORM, entityId=attachNo)
        List<String> keys = attachmentRepository.findObjectKeysByRef(comId, AttachmentDomain.DOC_FORM, attachNo);
        keys = (keys == null) ? List.of()
                : keys.stream().filter(k -> k != null && !k.isBlank()).distinct().toList();

        // 4) S3 먼저 삭제 (부분 실패까지 체크)
        deleteObjectsFromS3(keys);

        // 5) attachment DB 하드삭제
        attachmentRepository.hardDeleteByRef(comId, AttachmentDomain.DOC_FORM, attachNo);

        // 6) attach_box DB 하드삭제 (기존 repo 메서드 활용)
        int deleted = attachBoxRepository.deleteByAttachNoAndComId(attachNo, comId);
        if (deleted == 0) {
            // 여기까지 왔는데 0이면 동시 삭제 등 레이스일 수 있음
            throw new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }

        return new ResAttachDelDto(attachNo, true);
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

                // 부분 실패 체크
                if (resp != null && resp.hasErrors() && !resp.errors().isEmpty()) {
                    throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
                }

            } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
                throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
            }
        }
    }

    private ResAttachDetailDto toDetail(AttachBox a) {
        return new ResAttachDetailDto(
                a.getAttachNo(),
                a.getTitle(),
                a.getDscp(),
                a.getUploader(),
                a.getSize(),
                a.getPath()
        );
    }

    private String requireComId(CustomUser user) {
        String comId = (user == null) ? null : user.getComId();
        if (comId == null || comId.isBlank()) {
            throw new RuntimeException("회사 정보(comId)가 없습니다.");
        }
        return comId.trim();
    }

    private String requireUploader(CustomUser user) {
        String uploader = (user == null) ? null : user.getUsername();
        if (uploader == null || uploader.isBlank()) {
            throw new RuntimeException("업로더 정보(username)가 없습니다.");
        }
        return uploader.trim();
    }

    @Scheduled(cron = "0 */3 * * * *") // 3분마다
    @Transactional
    public void cleanupUncommitted() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        attachBoxRepository.deleteByCommittedFalseAndCreatedAtBefore(threshold);
    }

    @Override
    @Transactional
    public void commit(Long attachNo, ReqAttachCommitDto req, CustomUser user) {
        String comId = requireComId(user);

        AttachBox a = attachBoxRepository
                .findByAttachNoAndCompany_ComId(attachNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));

        if (req != null) a.updateSize(req.size());
        a.commit();
    }

    @Override
    @Transactional
    public void update(Long attachNo, ReqAttachUpdateDto req, CustomUser user) {
        String comId = requireComId(user);

        if (attachNo == null || attachNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        if (req == null) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        if (req.title() == null || req.title().isBlank())
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TITLE_REQUIRED);

        AttachBox a = attachBoxRepository
                .findByAttachNoAndCompany_ComId(attachNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));

        a.updateMeta(req.title().trim(), (req.dscp() == null ? null : req.dscp().trim()));
    }
}