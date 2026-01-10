package com.multi.mlpenterpriseapprovalsystem.search.service;

import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchDocMapperRegistry
 * @since : 2026. 1. 9. 금요일
 */
@Component
@RequiredArgsConstructor
public class SearchDocMapperRegistry {

    private final List<SearchDocMapper> mappers;

    public SearchDocMapper get(SearchDocType type) {
        return mappers.stream()
                .filter(m -> m.supports() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No SearchDocMapper for type=" + type));
    }
}
