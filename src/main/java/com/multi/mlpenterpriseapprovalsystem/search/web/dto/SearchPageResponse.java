package com.multi.mlpenterpriseapprovalsystem.search.web.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchPageResponse
 * @since : 2026. 1. 10. 토요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchPageResponse {
    private String query;
    private long total;
    private int page;   // 0-based
    private int size;

    @Builder.Default
    private List<SearchGroupedResponse.Item> items = new ArrayList<>();
}