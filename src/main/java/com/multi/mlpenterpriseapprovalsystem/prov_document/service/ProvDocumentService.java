package com.multi.mlpenterpriseapprovalsystem.prov_document.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.client.EmbeddingClient;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.AttachmentServiceImpl;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.S3UrlService;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.prov_document.domain.ProvDocument;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.*;
import com.multi.mlpenterpriseapprovalsystem.prov_document.repository.ProvDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사내규정 서비스
 *
 * @author : 김승기
 * @filename : ProvDocumentService
 * @since : 2025. 12. 29. 월요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProvDocumentService {

    private final ProvDocumentRepository provDocumentRepository;
    private final CompanyRepository companyRepository;
    private final EmbeddingClient embeddingClient;
    private final S3UrlService s3UrlService;
    private final AttachmentServiceImpl attachmentService;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    public Long create(String comId, ReqProvDocumentCreateDto req) {
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 파일 정보는 나중에 ai-request 단계에서 업데이트하므로 초기엔 null/0으로 설정
        ProvDocument doc = ProvDocument.create(
                company,
                req.getDocTitle(),
                req.getDescription(),
                req.getIsPublic(),
                null,
                0L
        );

        return provDocumentRepository.save(doc).getProvNo();
    }

    /**
     * 2. AI 분석 요청 (MeetingService.requestAiPipeline 방식)
     * 공통 첨부파일 API를 통해 S3 업로드가 완료된 후 호출됩니다.
     */
    @Transactional
    public Long completeAndRequestEmbedding(String comId, ReqProvDocumentCompleteDto req) {
        ProvDocument doc = provDocumentRepository.findById(req.getProvNo())
                .orElseThrow(() -> new CustomException(ErrorCode.PROV_DOCUMENT_NOT_FOUND));

        if (doc.getCompany() == null || !comId.equals(doc.getCompany().getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 파일 정보 업데이트 및 상태 변경
        doc.markUploaded(req.getOriginalName(), req.getObjectKey(), req.getSize());
        doc.markProcessing();

        // AI 임베딩 요청 (이벤트 방식이 권장되나 기존 코드 유지)
        ReqFastApiProvEmbeddingDto embeddingReq = ReqFastApiProvEmbeddingDto.builder()
                .provNo(doc.getProvNo())
                .comId(comId)
                .objectKey(req.getObjectKey())
                .originalName(req.getOriginalName())
                .contentType(req.getContentType())
                .size(req.getSize())
                .callbackUrl("/api/v1/prov-documents/" + doc.getProvNo() + "/embedding")
                .build();

        embeddingClient.requestProvEmbedding(embeddingReq);

        return doc.getProvNo();
    }

    @Transactional(readOnly = true)
    public Page<ResProvDocumentListItemDto> getList(String comId, String keyword, Boolean isPublic, Pageable pageable) {
        Page<ProvDocument> page = provDocumentRepository.searchDocuments(comId, keyword, isPublic, pageable);

        return page.map(this::toListItemDto);
    }

    @Transactional(readOnly = true)
    public ResProvDocumentDetailDto getDetail(String comId, Long provNo) {
        ProvDocument doc = provDocumentRepository.findById(provNo)
                .orElseThrow(() -> new CustomException(ErrorCode.PROV_DOCUMENT_NOT_FOUND));

        if (doc.getCompany() == null || !comId.equals(doc.getCompany().getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String downloadUrl = null;
        if (doc.getObjectKey() != null && !doc.getObjectKey().isBlank()) {
            downloadUrl = s3UrlService.presignGetUrl(doc.getObjectKey()); // 너 메서드명 맞게
        }

        return ResProvDocumentDetailDto.builder()
                .provNo(doc.getProvNo())
                .docTitle(doc.getDocTitle())
                .description(doc.getDescription())
                .isPublic(doc.getIsPublic())
                .fileName(doc.getFileName())
                .objectKey(doc.getObjectKey())
                .fileSize(doc.getFileSize())
                .chunkCnt(doc.getChunkCnt())
                .procStat(doc.getProcStat())
                .errorMsg(doc.getErrorMsg())
                .downloadUrl(downloadUrl)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    @Transactional
    public Long update(String comId, Long provNo, ReqProvDocumentUpdateDto req) {

        ProvDocument doc = provDocumentRepository.findById(provNo)
                .orElseThrow(() -> new CustomException(ErrorCode.PROV_DOCUMENT_NOT_FOUND));

        if (doc.getCompany() == null || !comId.equals(doc.getCompany().getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String newTitle = (req.getDocTitle() != null) ? req.getDocTitle() : doc.getDocTitle();
        String newDesc = (req.getDescription() != null) ? req.getDescription() : doc.getDescription();
        Boolean newPublic = (req.getIsPublic() != null) ? req.getIsPublic() : doc.getIsPublic();

        doc.updateMeta(newTitle, newDesc, newPublic);

        return doc.getProvNo();
    }

    @Transactional
    public Long delete(String comId, Long provNo, CustomUser user) {

        ProvDocument doc = provDocumentRepository.findById(provNo)
                .orElseThrow(() -> new CustomException(ErrorCode.PROV_DOCUMENT_NOT_FOUND));

        if (doc.getCompany() == null || !comId.equals(doc.getCompany().getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        try {
            embeddingClient.deleteProvEmbedding(
                    ReqFastApiProvDeleteDto.builder()
                            .comId(comId)
                            .provNo(provNo)
                            .build()
            );
        } catch (Exception e) {
            log.error("Embedding vector delete FAILED. comId={}, provNo={}, msg={}",
                    comId, provNo, e.getMessage(), e);
            throw new CustomException(ErrorCode.EMBEDDING_DELETE_FAILED);
        }
        attachmentRepository.findAllByRefForUpdate(comId, AttachmentDomain.PROV_DOCUMENT, provNo)
                .forEach(attachment -> {
                    // attachmentService.softDelete()를 호출하여 S3와 DB 데이터를 삭제합니다.
                    // 위에서 수정한 로직 덕분에 PROV_DOCUMENT 도메인은 하드 삭제가 진행됩니다.
                    attachmentService.softDelete(attachment.getAttachmentId(), user);
                });
        provDocumentRepository.delete(doc);
        return provNo;
    }

    @Transactional
    public Long applyEmbeddingResult(Long provNo, ReqProvEmbeddingCallbackDto request) {
        ProvDocument doc = provDocumentRepository.findById(provNo)
                .orElseThrow(() -> new CustomException(ErrorCode.PROV_DOCUMENT_NOT_FOUND));

        log.info("EMBEDDING CALLBACK provNo={}, success={}, chunkCnt={}, errorMsg={}",
                provNo, request.getSuccess(), request.getChunkCnt(), request.getErrorMsg());

        if (Boolean.FALSE.equals(request.getSuccess())) {
            doc.markFailed(request.getErrorMsg());
            return doc.getProvNo();
        }

        doc.markDone(request.getChunkCnt() == null ? 0 : request.getChunkCnt());
        return doc.getProvNo();
    }

    private ResProvDocumentListItemDto toListItemDto(ProvDocument doc) {
        return ResProvDocumentListItemDto.builder()
                .provNo(doc.getProvNo())
                .docTitle(doc.getDocTitle())
                .description(doc.getDescription())
                .isPublic(doc.getIsPublic())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .procStat(doc.getProcStat())
                .chunkCnt(doc.getChunkCnt())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ResProvDocumentListItemDto> getPublicDoneList(String comId, Pageable pageable) {
        // isPublic = true, procStat = "DONE" 조건을 고정해서 전달
        Page<ProvDocument> page = provDocumentRepository.findChatbotAvailableDocuments(comId, pageable);

        return page.map(this::toListItemDto);
    }
}