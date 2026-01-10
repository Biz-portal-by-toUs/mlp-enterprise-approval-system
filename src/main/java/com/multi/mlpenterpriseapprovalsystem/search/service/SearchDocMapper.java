package com.multi.mlpenterpriseapprovalsystem.search.service;

import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.dto.SearchDocument;

import java.util.Optional;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchDocMapper
 * @since : 2026. 1. 9. 금요일
 */
public interface SearchDocMapper {
    SearchDocType supports();
    Optional<SearchDocument> buildUpsert(String comId, String sourceId);
    default String docId(String comId, SearchDocType type, String sourceId) {
        return comId + ":" + type.name() + ":" + sourceId;
    }
}