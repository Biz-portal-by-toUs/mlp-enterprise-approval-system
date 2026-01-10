package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.common.*;
import com.multi.mlpenterpriseapprovalsystem.common.exception.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.service.*;
import jakarta.validation.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.*;
import org.springframework.security.core.annotation.*;
import org.springframework.web.bind.annotation.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : AttachBoxController
 * @since : 2026-01-10 토요일
 */

@RestController
@RequestMapping("/api/v1/attach-box")
@RequiredArgsConstructor
public class AttachBoxController {

    private final AttachBoxService attachBoxService;

    // 첨부 등록 (메타데이터 저장)
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @PostMapping
    public ResponseEntity<ResponseDto<Long>> create(
            @AuthenticationPrincipal CustomUser customUser,
            @Valid @RequestBody ReqAttachCreateDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        Long attachNo = attachBoxService.create(req, customUser);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "첨부 등록 성공", attachNo));
    }

    // 첨부 목록 조회 (내 회사 것만)
    @GetMapping
    public ResponseEntity<ResponseDto<Page<ResAttachListDto>>> list(
            @AuthenticationPrincipal CustomUser customUser,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        Page<ResAttachListDto> res = attachBoxService.list(pageable, customUser);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "첨부 목록 조회 성공", res));
    }

    // 첨부 상세 조회 (내 회사 것만)
    @GetMapping("/{attachNo}")
    public ResponseEntity<ResponseDto<ResAttachDetailDto>> detail(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "attachNo") Long attachNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (attachNo == null || attachNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        ResAttachDetailDto res = attachBoxService.detail(attachNo, customUser);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "첨부 상세 조회 성공", res));
    }

    // 첨부 삭제 (내 회사 범위만, 작성자 제한 없음)
    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @DeleteMapping("/{attachNo}")
    public ResponseEntity<ResponseDto<ResAttachDelDto>> delete(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "attachNo") Long attachNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (attachNo == null || attachNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        ResAttachDelDto res = attachBoxService.delete(attachNo, customUser);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "첨부 삭제 성공", res));
    }
}