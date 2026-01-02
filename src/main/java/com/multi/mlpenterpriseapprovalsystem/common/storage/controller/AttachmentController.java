package com.multi.mlpenterpriseapprovalsystem.common.storage.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.AttachmentDto;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.AttachmentPresignService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.AttachmentQueryService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final AttachmentQueryService attachmentQueryService;


    @PostMapping("/presign")
    public ResponseEntity<ResponseDto<AttachmentDto.PresignResponse>> presign(
            @RequestBody AttachmentDto.PresignRequest req,
            @AuthenticationPrincipal CustomUser user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseDto<>(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", null));
        }
        String comId = user.getComId();
        AttachmentDto.PresignResponse result = presignService.presignPut(req, comId);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "s3 경로 key 생성", result));
    }

    @PostMapping("/complete")
    public ResponseEntity<ResponseDto<AttachmentDto.CompleteResponse>> complete(
            @RequestBody AttachmentDto.CompleteRequest req,
            @AuthenticationPrincipal CustomUser user
    ) {
        AttachmentDto.CompleteResponse res = attachmentService.completeUpload(req, user);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "첨부 업로드 확정(DB 저장 완료)", res));
    }

    public record DeleteResponse(Long attachmentId) {}

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<ResponseDto<DeleteResponse>> delete(
            @PathVariable(name="attachmentId") Long attachmentId,
            @AuthenticationPrincipal CustomUser user
    ) {
        Long deletedId = attachmentService.softDelete(attachmentId, user);
        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "첨부파일 삭제 완료", new DeleteResponse(deletedId))
        );
    }

    // 목록: /api/v1/attachments?domain=NOTICE&entityId=88
    @GetMapping
    public ResponseEntity<ResponseDto<List<AttachmentDto.AttachmentListItem>>> list(
            @RequestParam(name="domain") AttachmentDomain domain,
            @RequestParam(name="entityId") Long entityId,
            @AuthenticationPrincipal CustomUser user
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "첨부파일 목록 반환 성공",
                        attachmentQueryService.list(user.getComId(), domain, entityId)));
    }

    // 미리보기 URL
    @GetMapping("/{attachmentId}/preview-url")
    public ResponseEntity<ResponseDto<AttachmentDto.PresignedUrlResponse>> previewUrl(
            @PathVariable(name="attachmentId") Long attachmentId,
            @AuthenticationPrincipal CustomUser user
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "미리보기 성공",
                attachmentQueryService.issuePreviewUrl(user.getComId(), attachmentId))
        );
    }

    // 다운로드 URL
    @GetMapping("/{attachmentId}/download-url")
    public ResponseEntity<ResponseDto<AttachmentDto.PresignedUrlResponse>> downloadUrl(
            @PathVariable(name="attachmentId") Long attachmentId,
            @AuthenticationPrincipal CustomUser user
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "다운로드 성공",
                attachmentQueryService.issueDownloadUrl(user.getComId(), attachmentId))
        );
    }

    @PostMapping("/{attachmentId}/move")
    public ResponseEntity<ResponseDto<Void>> move(
            @PathVariable Long attachmentId,
            @RequestParam Long toFolderNo,
            @AuthenticationPrincipal CustomUser user
    ){
        attachmentService.moveCloudAttachment(user, attachmentId, toFolderNo);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "이동 성공", null));
    }
}
