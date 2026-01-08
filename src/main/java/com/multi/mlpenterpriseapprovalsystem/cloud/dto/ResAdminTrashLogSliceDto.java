package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관리자 휴지통(Trash) 로그 목록을 "슬라이스(Slice)" 방식으로 응답하기 위한 DTO
 *
 *  Slice 방식이란?
 * - 전체 개수(totalCount)나 전체 페이지(totalPages)를 내려주지 않고,
 *   "다음 데이터가 더 있는지(hasNext)"만 알려주는 페이징 방식
 * - 무한 스크롤 / 더보기 UI에 적합
 *
 *  보통 hasNext 계산 방법
 * - DB에서 limit + 1 개를 조회한다.
 *   - 조회 결과가 limit 보다 많으면 → hasNext = true (다음 페이지 있음)
 *   - 조회 결과가 limit 이하이면 → hasNext = false (다음 페이지 없음)
 * - 응답 items에는 실제로 limit 개만 잘라서 내려준다.
 *
 * @author : 송현님
 * @filename : ResAdminTrashLogListDto
 * @since : 2026-01-07 오후 11:49 수요일
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResAdminTrashLogSliceDto {
    /** 현재 페이지 데이터 */
    private List<ResAdminTrashLogDto> items;

    /** 다음 페이지 존재 여부 (limit+1 조회로 계산) */
    private boolean hasNext;

    /** 요청 limit (보통 10) */
    private int limit;

    /** 요청 offset (page 기반이면 (page-1)*limit) */
    private int offset;
}
