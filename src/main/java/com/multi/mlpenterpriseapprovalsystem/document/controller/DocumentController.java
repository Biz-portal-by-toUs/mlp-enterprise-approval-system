package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqApprovalLineDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 문서 처리 컨트롤러
 *
 * @author : 이지헌
 * @filename : DocumentController
 * @since : 25. 12. 15. 월요일
 */

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;

    // 회사 전체의 문서를 문서상태 기준 조회
    @GetMapping("/documents")
    public ResponseEntity<ResponseDto<Page<ResDocumentDto>>> getDocumentsByStatus(@AuthenticationPrincipal CustomUser customUser,
                                                                                  @ModelAttribute ReqDocumentDto reqDocumentDto,
                                                                                  @RequestParam(name = "status") String status,
                                                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                                                  @RequestParam(name = "sort", defaultValue = "") String sort) {

        Page<ResDocumentDto> resDocumentDtos = documentService.getDocumentsByStatus(customUser.getComId(), customUser.getUsername(), reqDocumentDto, status, page, sort);

        String message = "";
        if ("FINALIZED".equals(status)) {
            message = resDocumentDtos.isEmpty() ? "최종 승인된 문서가 없습니다" : "최종 승인된 문서 조회 성공";
        }
        else {
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, resDocumentDtos));
    }

    // 임시저장한 문서 조회(status = "UNSUBMITTED), 내가 상신한 문서 조회(status = "SUBMITTED"),
    // 내가 결재할 문서 조회(status = "AWAITING"), 내가 결재한 문서 조회(status = "PROCESSED")
    @GetMapping("/documents/me")
    public ResponseEntity<ResponseDto<Page<ResDocumentDto>>> getMyDocumentsByStatus(@AuthenticationPrincipal CustomUser customUser,
                                                                                    @ModelAttribute ReqDocumentDto reqDocumentDto,
                                                                                    @RequestParam(name = "status") String status,
                                                                                    @RequestParam(name = "page", defaultValue = "0") int page,
                                                                                    @RequestParam(name = "sort", defaultValue = "") String sort) {

        Page<ResDocumentDto> resDocumentDtos = documentService.getMyDocumentsByStatus(customUser.getComId(), customUser.getUsername(), reqDocumentDto, status, page, sort);

        String message = "";
        if("UNSUBMITTED".equals(status)){
            message = resDocumentDtos.isEmpty() ? "임시저장한 문서가 없습니다" : "임시저장한 문서 조회 성공";
        }
        else if ("SUBMITTED".equals(status)) {
            message = resDocumentDtos.isEmpty() ? "내가 상신한 문서가 없습니다" : "내가 상신한 문서 조회 성공";
        }
        else if ("AWAITING".equals(status)) {
            message = resDocumentDtos.isEmpty() ? "내가 결재할 문서가 없습니다" : "내가 결재할 문서 조회 성공";
        }
        else if ("PROCESSED".equals(status)) {
            message = resDocumentDtos.isEmpty() ? "내가 결재한 문서가 없습니다" : "내가 결재한 문서 조회 성공";
        }
        else {
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, resDocumentDtos));
    }


    // 문서식별자로 문서 상세조회
    @GetMapping("/documents/{docNo}")
    public ResponseEntity<ResponseDto<ResDocumentDto>> getDocumentByDocNo(@PathVariable(name = "docNo") Long docNo,
                                                                          @RequestParam(name = "status") String status,
                                                                          @AuthenticationPrincipal CustomUser customUser) {

        if (!"UNSUBMITTED".equals(status) && !"SUBMITTED".equals(status) && !"FINALIZED".equals(status) && !"PROCESSED".equals(status) && !"AWAITING".equals(status)) {
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }

        ResDocumentDto resDocumentDto = documentService.getDocumentByDocNoWithStatus(customUser.getComId(), customUser.getUsername(), docNo, status);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "문서 조회 성공", resDocumentDto));
    }

    // 문서 상신 및 임시저장
    @PostMapping("/documents")
    public ResponseEntity<ResponseDto<Void>> createDocument(@AuthenticationPrincipal CustomUser customUser,
                                                            @Valid @RequestBody ReqDocumentDto reqDocumentDto) {
        documentService.createDocument(customUser.getComId(), /*empId*/customUser.getUsername(), reqDocumentDto);

        String message = Boolean.TRUE.equals(reqDocumentDto.getTemp())
                ? "문서 임시저장 성공"
                : "문서 상신 성공";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, message, null));
    }

    // 결재자 없을 시 상신 취소. 상신일 null로 변경, 임시저장상태를 true로 변경, 문서상태를 상신전(US)로 변경.
    @PatchMapping("/documents/{docNo}/cancel")
    public ResponseEntity<ResponseDto<Void>> cancelSubmit(@PathVariable(name = "docNo") Long docNo,
                                                          @AuthenticationPrincipal CustomUser customUser) {
        String comId = customUser.getComId();
        String myEmpId = customUser.getUsername();

        documentService.cancelSubmit(comId, myEmpId, docNo);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "상신 취소 성공", null));
    }

    // 결재 승인 및 반려
    // 승인 시 결재시간(endedAt)에 현재시간 넣고 내 결재상태를 승인(A)로 변경, 결재순서를 다음사람한테 패스, 최종승인이면 문서코드 발행
    // 반려 시 결재시간(endedAt)에 현재시간 넣고 내 결재상태를 반려(R)로 변경
    // 문서상태를 내 결재까지 포함해서 갱신
    @PatchMapping("/documents/{docNo}/process")
    public ResponseEntity<ResponseDto<Void>> processApproval(@PathVariable(name = "docNo") Long docNo,
                                                             @AuthenticationPrincipal CustomUser customUser,
                                                             @RequestBody ReqApprovalLineDto reqApprovalLineDto) {
        String comId = customUser.getComId();
        String myEmpId = customUser.getUsername();

        documentService.processApproval(comId, myEmpId, docNo, reqApprovalLineDto);

        String message = "A".equals(reqApprovalLineDto.getApprStat()) ? "결재가 승인되었습니다" : "결재가 반려되었습니다";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, null));
    }


    /**
     * 반려된 문서 재작성 (새 문서로 생성)
     * - 기존 반려 문서는 유지, 새 문서 생성 (INSERT)
     */
    @PostMapping("/documents/{docNo}/resubmit")
    public ResponseEntity<ResponseDto<Void>> resubmitRejectedDocument(@PathVariable(name = "docNo") Long docNo,
                                                                      @AuthenticationPrincipal CustomUser customUser,
                                                                      @Valid @RequestBody ReqDocumentDto reqDocumentDto) {
        String comId = customUser.getComId();
        String myEmpId = customUser.getUsername();

        documentService.resubmitRejectedDocument(comId, myEmpId, docNo, reqDocumentDto);

        String message = Boolean.TRUE.equals(reqDocumentDto.getTemp())
                ? "문서가 임시저장되었습니다"
                : "문서가 재상신되었습니다";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, message, null));
    }


    /**
     * 임시저장 문서 수정
     * - 기존 문서를 수정 (UPDATE)
     */
    @PutMapping("/documents/{docNo}")
    public ResponseEntity<ResponseDto<Void>> updateTempDocument(@PathVariable(name = "docNo") Long docNo,
                                                                @AuthenticationPrincipal CustomUser customUser,
                                                                @Valid @RequestBody ReqDocumentDto reqDocumentDto) {
        String comId = customUser.getComId();
        String myEmpId = customUser.getUsername();

        documentService.updateTempDocument(comId, myEmpId, docNo, reqDocumentDto);

        String message = Boolean.TRUE.equals(reqDocumentDto.getTemp())
                ? "문서가 임시저장되었습니다"
                : "문서가 상신되었습니다";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, null));
    }




}
