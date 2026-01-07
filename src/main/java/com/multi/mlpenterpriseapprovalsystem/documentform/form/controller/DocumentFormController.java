package com.multi.mlpenterpriseapprovalsystem.documentform.form.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.res.ResDocumentFormDetailDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.res.ResDocumentFormListDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.enums.DocumentFormStats;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.service.DocumentFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 문서양식 REST API 컨트롤러
 *
 * @author : 정종원
 * @filename : DocumentFormController
 * @since : 2025-12-22 월요일
 */
@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class DocumentFormController {

    private final DocumentFormService documentFormService;

    // 문서양식 생성
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @PostMapping
    public ResponseEntity<ResponseDto<Long>> createDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @Valid @RequestBody ReqDocumentFormCreateDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        Long docfoNo = documentFormService.createDocumentForm(
                req, customUser.getComId(), customUser.getUsername()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 생성 성공", docfoNo));
    }

    // 문서양식 수정
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @PutMapping("/{docfoNo}")
    public ResponseEntity<ResponseDto<Long>> updateDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo,
            @Valid @RequestBody ReqDocumentFormCreateDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        Long newDocfoNo = documentFormService.updateDocumentForm(
                docfoNo, req, customUser.getComId(), customUser.getUsername()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 수정 성공", newDocfoNo));
    }

    // 삭제 요청
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @DeleteMapping("/{docfoNo}")
    public ResponseEntity<ResponseDto<Void>> requestDelete(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        documentFormService.requestDelete(docfoNo, customUser.getComId(), customUser);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 삭제 요청 성공", null));
    }

    // 목록 조회
    @GetMapping
    public ResponseEntity<ResponseDto<Page<ResDocumentFormListDto>>> getForms(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "stat", defaultValue = "A") String stat,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "docfoName", required = false) String docfoName,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        String keyword = (q != null && !q.isBlank()) ? q : docfoName;

        List<DocumentFormStats> stats = List.of(stat.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(DocumentFormStats::valueOf)
                .toList();

        if (CollectionUtils.isEmpty(stats)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Page<ResDocumentFormListDto> res =
                documentFormService.findListByStatuses(stats, customUser.getComId(), keyword, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 목록 조회 성공", res));
    }

    // 상세 조회
    @GetMapping("/{docfoNo}")
    public ResponseEntity<ResponseDto<ResDocumentFormDetailDto>> getDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        ResDocumentFormDetailDto res =
                documentFormService.findDetailById(docfoNo, customUser.getComId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 상세 조회 성공", res));
    }

    // 상태 변경 (승인/반려 등)
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN')")
    @PatchMapping("/{docfoNo}/status")
    public ResponseEntity<ResponseDto<Void>> changeStatus(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo,
            @Valid @RequestBody ReqDocumentFormStatusDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        documentFormService.changeApproveOrReject(
                docfoNo,
                customUser.getComId(),
                req.docfoStat(),
                req.rejectReason()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 상태 변경 성공", null));
    }

    // 삭제 승인
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN')")
    @PatchMapping("/{docfoNo}/delete-approve")
    public ResponseEntity<ResponseDto<Void>> approveDelete(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        documentFormService.approveDelete(docfoNo, customUser.getComId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 삭제 승인 성공", null));
    }

    // 삭제 반려
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN')")
    @PatchMapping("/{docfoNo}/delete-reject")
    public ResponseEntity<ResponseDto<Void>> rejectDelete(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo,
            @Valid @RequestBody ReqDocumentFormStatusDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        if (req.rejectReason() == null || req.rejectReason().isBlank()) {
            throw new CustomException(ErrorCode.REJECT_REASON_REQUIRED);
        }

        documentFormService.rejectDelete(docfoNo, customUser.getComId(), req.rejectReason());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 삭제 반려 성공", null));
    }

    // 임시저장 생성
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @PostMapping("/temp")
    public ResponseEntity<ResponseDto<Long>> createTemp(
            @AuthenticationPrincipal CustomUser customUser,
            @Valid @RequestBody ReqDocumentFormTempDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        Long docfoNo = documentFormService.createTemp(
                req, customUser.getComId(), customUser.getUsername()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 임시저장 생성 성공", docfoNo));
    }

    // 임시저장 저장/수정
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @PutMapping("/{docfoNo}/temp")
    public ResponseEntity<ResponseDto<Void>> saveTemp(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo,
            @Valid @RequestBody ReqDocumentFormTempDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        documentFormService.saveTemp(
                docfoNo, req, customUser.getComId(), customUser.getUsername()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 임시저장 저장 성공", null));
    }

    // 내 임시저장 목록
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/temp")
    public ResponseEntity<ResponseDto<Page<ResDocumentFormListDto>>> getMyTempForms(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "q", required = false) String q,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        Page<ResDocumentFormListDto> res =
                documentFormService.findMyTempList(
                        customUser.getComId(), customUser.getUsername(), q, pageable
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 임시저장 목록 조회 성공", res));
    }

    // 임시저장 삭제(완전삭제)
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @DeleteMapping("/temp/{docfoNo}")
    public ResponseEntity<ResponseDto<Void>> deleteTemp(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        documentFormService.deleteTemp(docfoNo, customUser.getComId(), customUser.getUsername());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 임시저장 삭제 성공", null));
    }
}
