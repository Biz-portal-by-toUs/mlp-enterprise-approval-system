package com.multi.mlpenterpriseapprovalsystem.prov_document.service;

import com.multi.mlpenterpriseapprovalsystem.common.client.EmbeddingClient;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.S3UrlService;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.prov_document.domain.ProvDocument;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.*;
import com.multi.mlpenterpriseapprovalsystem.prov_document.repository.ProvDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    private final EmbeddingClient embeddingClient; // Spring -> FastAPI
    private final S3UrlService s3UrlService;

    @Transactional
    public ResProvDocumentCreateDto createAndPresign(String empId, ReqProvDocumentCreateDto req) {

        Employee me = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String comId = me.getCompany().getComId();

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        ProvDocument doc = ProvDocument.create(
                company,
                req.getDocTitle(),
                req.getDescription(),
                req.getIsPublic(),
                req.getOriginalName(),
                req.getSize()
        );

        ProvDocument saved = provDocumentRepository.save(doc); // ✅ 여기서 provNo 발급

        String objectKey = buildObjectKey(comId, saved.getProvNo(), req.getOriginalName());
        saved.assignObjectKey(objectKey);

        String uploadUrl = s3UrlService.presignPutUrl(objectKey, req.getContentType());

        return ResProvDocumentCreateDto.builder()
                .provNo(saved.getProvNo())
                .objectKey(objectKey)
                .uploadUrl(uploadUrl)
                .procStat(saved.getProcStat())
                .build();
    }

    @Transactional
    public Long completeAndRequestEmbedding(String empId, ReqProvDocumentCompleteDto req) {

        Employee me = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String comId = me.getCompany().getComId();

        ProvDocument doc = provDocumentRepository.findById(req.getProvNo())
                .orElseThrow(() -> new CustomException(ErrorCode.PROV_DOCUMENT_NOT_FOUND));

        if (doc.getCompany() == null || !comId.equals(doc.getCompany().getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String expectedPrefix = "prov-documents/" + comId + "/" + doc.getProvNo() + "/";
        if (req.getObjectKey() == null || !req.getObjectKey().startsWith(expectedPrefix)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String stableFileRef = req.getObjectKey();

        doc.markUploaded(req.getOriginalName(), stableFileRef, req.getSize());
        doc.markProcessing();

        ReqFastApiProvEmbeddingDto embeddingReq = ReqFastApiProvEmbeddingDto.builder()
                .provNo(doc.getProvNo())
                .comId(comId)
                .objectKey(req.getObjectKey())
                .originalName(req.getOriginalName())
                .contentType(req.getContentType())
                .size(req.getSize())
                .callbackUrl("/api/v1/prov-documents/" + doc.getProvNo() + "/embedding") // 콜백 엔드포인트명도 embedding으로 추천
                .build();

        embeddingClient.requestProvEmbedding(embeddingReq);

        return doc.getProvNo();
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

    private String buildObjectKey(String comId, Long provNo, String originalName) {
        return "prov-documents/" + comId + "/" + provNo + "/" + originalName;
    }
}