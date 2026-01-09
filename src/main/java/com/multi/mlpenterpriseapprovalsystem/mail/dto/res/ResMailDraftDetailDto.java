package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import java.time.*;
import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ResMailDraftDetailDto
 * @since : 2026-01-08 목요일
 */
public record ResMailDraftDetailDto(
        Long mailNo,
        String mailId,
        String title,
        String cnttJson,
        LocalDateTime savedAt,
        List<String> receiverEmpIds,
        List<String> receiverNames,
        String receivers
) {}
