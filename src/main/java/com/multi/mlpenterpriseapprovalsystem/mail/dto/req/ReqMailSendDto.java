package com.multi.mlpenterpriseapprovalsystem.mail.dto.req;

import java.util.*;

/**
 * 메일 발송 Dto
 *
 * @author : 정종원
 * @filename : ReqMailSendDto
 * @since : 2025-12-30 화요일
 */

public record ReqMailSendDto(
        String title,
        String cnttJson,
        List<String> receiverEmpIds
) {}