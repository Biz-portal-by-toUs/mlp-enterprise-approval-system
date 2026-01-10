package com.multi.mlpenterpriseapprovalsystem.mail.dto.req;

import java.util.*;

/**
 * 임시저장된 메일 발송 요청 Dto
 *
 * @author : 정종원
 * @filename : ReqMailDraftSendDto
 * @since : 2026-01-07 수요일
 */

public record ReqMailDraftSendDto(
        String title,
        String cnttJson,
        List<String> receiverEmpIds
) {}
