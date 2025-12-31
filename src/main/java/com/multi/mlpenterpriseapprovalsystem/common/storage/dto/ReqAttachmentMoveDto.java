package com.multi.mlpenterpriseapprovalsystem.common.storage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : ReqAttachmentMoveDto
 * @since : 2025-12-30 오후 3:15 화요일
 */

@Getter
@Setter
public class ReqAttachmentMoveDto {

    @NotNull
    private Long toFolderNo;
}
