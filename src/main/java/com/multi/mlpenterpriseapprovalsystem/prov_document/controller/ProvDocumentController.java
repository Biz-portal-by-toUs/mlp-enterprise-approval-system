package com.multi.mlpenterpriseapprovalsystem.prov_document.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.*;
import com.multi.mlpenterpriseapprovalsystem.prov_document.service.ProvDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasRole('COM_ADMIN')")
    @PostMapping
    public ResponseEntity<ResponseDto<Long>> create(
            @RequestBody @Valid ReqProvDocumentCreateDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        // 반환 타입을 Long으로 변경하여 provNo만 넘겨줍니다.
        Long provNo = provDocumentService.create(user.getComId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "규정 정보 생성 성공", provNo));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @PostMapping("/complete")
    public ResponseEntity<ResponseDto<Long>> complete(
            @RequestBody @Valid ReqProvDocumentCompleteDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        // 이름은 유지하되 로직은 AI 분석 요청만 수행합니다.
        Long provNo = provDocumentService.completeAndRequestEmbedding(user.getComId(), request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "AI 임베딩 요청 성공", provNo));
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

    @PreAuthorize("hasRole('COM_ADMIN')")
    @GetMapping
    public ResponseEntity<ResponseDto<Page<ResProvDocumentListItemDto>>> getList(
            @RequestParam(name = "isPublic", required = false) Boolean isPublic,
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        Page<ResProvDocumentListItemDto> page = provDocumentService.getList(user.getComId(), keyword, isPublic, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "규정 목록 조회 성공", page));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @GetMapping("/{provNo}")
    public ResponseEntity<ResponseDto<ResProvDocumentDetailDto>> getDetail(
            @PathVariable(name = "provNo") Long provNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        ResProvDocumentDetailDto detail = provDocumentService.getDetail(user.getComId(), provNo);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "규정 상세 조회 성공", detail));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @PatchMapping("/{provNo}")
    public ResponseEntity<ResponseDto<Long>> update(
            @PathVariable(name = "provNo") Long provNo,
            @RequestBody @Valid ReqProvDocumentUpdateDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        if (request.getProvNo() == null || !request.getProvNo().equals(provNo)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Long updated = provDocumentService.update(user.getComId(), provNo, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "규정 수정 성공", updated));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @DeleteMapping("/{provNo}")
    public ResponseEntity<ResponseDto<Long>> delete(
            @PathVariable(name = "provNo") Long provNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        Long deleted = provDocumentService.delete(user.getComId(), provNo, user);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "규정 삭제 성공", deleted));
    }
}