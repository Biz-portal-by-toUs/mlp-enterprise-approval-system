package com.multi.mlpenterpriseapprovalsystem.common.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 첨부파일(Attachment) 이름 변경 요청 DTO입니다.
 *
 * originalName은 확장자를 포함한 전체 파일명이며,
 * 서버는 해당 값을 Attachment.originalName / ext에 반영합니다.
 *
 * @author : 송현님
 * @filename : ReqRenameAttachmentDto
 * @since : 2026-01-07 오후 8:26 수요일
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqRenameAttachmentDto {
    private String originalName;
}
