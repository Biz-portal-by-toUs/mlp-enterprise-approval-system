package com.multi.mlpenterpriseapprovalsystem.common.storage.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.AttachmentPresignService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.AttachmentService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.AttachmentServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 첨부파일 s3 저장 경로 만드는 컨트롤러
 *
 * @author : 권지영
 * @filename : AttachmentController
 * @since : 2025. 12. 23. 화요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentPresignService presignService;
    private final AttachmentService attachmentService;

    @PostMapping("/presign")
    public ResponseEntity<ResponseDto<AttachmentPresignService.PresignResponse>> presign(
            @RequestBody AttachmentPresignService.PresignRequest req,
            @AuthenticationPrincipal CustomUser user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", null));
        }
        String comId = user.getComId();
        AttachmentPresignService.PresignResponse result = presignService.presignPut(req, comId);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "s3 경로 key 생성", result));
    }

    @PostMapping("/complete")
    public ResponseEntity<ResponseDto<AttachmentServiceImpl.CompleteResponse>> complete(
            @RequestBody AttachmentServiceImpl.CompleteRequest req,
            @AuthenticationPrincipal CustomUser user
    ) {
        AttachmentServiceImpl.CompleteResponse res = attachmentService.completeUpload(req, user);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "첨부 업로드 확정(DB 저장 완료)", res));
    }

    public record DeleteResponse(Long attachmentId) {}

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<ResponseDto<DeleteResponse>> delete(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUser user
    ) {
        Long deletedId = attachmentService.softDelete(attachmentId, user);
        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "첨부파일 삭제(soft delete) 완료", new DeleteResponse(deletedId))
        );
    }
}
