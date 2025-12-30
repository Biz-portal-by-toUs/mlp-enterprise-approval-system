package com.multi.mlpenterpriseapprovalsystem.prov_document.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.ReqProvEmbeddingCallbackDto;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.ReqProvDocumentCompleteDto;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.ReqProvDocumentCreateDto;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.ResProvDocumentCreateDto;
import com.multi.mlpenterpriseapprovalsystem.prov_document.service.ProvDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사내 규정 컨트롤러
 *
 * @author : 김승기
 * @filename : ProvDocumentController
 * @since : 2025. 12. 29. 월요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prov-documents")
public class ProvDocumentController {

    private final ProvDocumentService provDocumentService;

    @Value("${internal.ai.callback-key}")
    private String internalCallbackKey;


    @PostMapping
    public ResponseEntity<ResponseDto<ResProvDocumentCreateDto>> create(
            @RequestBody @Valid ReqProvDocumentCreateDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        ResProvDocumentCreateDto res = provDocumentService.createAndPresign(empId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "규정 등록(업로드URL 발급) 성공", res));
    }


    @PostMapping("/complete")
    public ResponseEntity<ResponseDto<Long>> complete(
            @RequestBody @Valid ReqProvDocumentCompleteDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        Long provNo = provDocumentService.completeAndRequestEmbedding(empId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "규정 업로드 완료 및 임베딩 요청 성공", provNo));
    }

    @PatchMapping("/{provNo}/embedding")
    public ResponseEntity<ResponseDto<Long>> embeddingCallback(
            @PathVariable(name = "provNo") Long provNo,
            @RequestBody @Valid ReqProvEmbeddingCallbackDto request,
            @RequestHeader(name = "X-Internal-Callback-Key") String callbackKey
    ) {
        if (request.getProvNo() == null || !request.getProvNo().equals(provNo)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (internalCallbackKey == null || !internalCallbackKey.equals(callbackKey)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Long updatedProvNo = provDocumentService.applyEmbeddingResult(provNo, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임베딩 성공", updatedProvNo));
    }
}