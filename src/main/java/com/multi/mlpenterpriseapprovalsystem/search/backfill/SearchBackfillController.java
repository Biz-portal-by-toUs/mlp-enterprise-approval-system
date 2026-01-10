package com.multi.mlpenterpriseapprovalsystem.search.backfill;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchBackfillController
 * @since : 2026. 1. 9. 금요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search/admin")
public class SearchBackfillController {

    private final SearchBackfillService backfillService;

    /**
     * 백필: 기존 RDB 데이터 -> outbox(PENDING) 적재
     * 그 다음은 SearchOutboxWorker가 ES로 반영
     */
    @PostMapping("/backfill")
    public ResponseEntity<ResponseDto<Map<String, Object>>> backfill(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(name = "comId", required = false) String comId
    ) {
        // comId 안 주면 내 회사로
        String targetComId = (comId == null || comId.isBlank()) ? user.getComId() : comId;

        Map<String, Object> result = backfillService.backfillAll(targetComId);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "백필(outbox 적재) 완료", result));
    }
}