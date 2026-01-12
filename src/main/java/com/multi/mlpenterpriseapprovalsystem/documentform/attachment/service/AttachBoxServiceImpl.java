package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.common.exception.*;
import com.multi.mlpenterpriseapprovalsystem.company.domain.*;
import com.multi.mlpenterpriseapprovalsystem.company.repository.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.domain.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.repository.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.*;

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

    @Override
    @Transactional
    public Long create(ReqAttachCreateDto req, CustomUser user) {
        String comId = requireComId(user);
        String uploader = requireUploader(user);

        if(req.title() == null || req.title().isEmpty())
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

    @Override
    @Transactional
    public ResAttachDelDto delete(Long attachNo, CustomUser user) {
        String comId = requireComId(user);

        AttachBox a = attachBoxRepository
                .findByAttachNoAndCompany_ComId(attachNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));

        attachBoxRepository.delete(a);

        return new ResAttachDelDto(attachNo, true);
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

    @Transactional
    public void commit(Long attachNo, ReqAttachCommitDto req, CustomUser user) {
        String comId = requireComId(user);

        AttachBox a = attachBoxRepository
                .findByAttachNoAndCompany_ComId(attachNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));

        if (req != null) a.updateSize(req.size());
        a.commit();
    }
}