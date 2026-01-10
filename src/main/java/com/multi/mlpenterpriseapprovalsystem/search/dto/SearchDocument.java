package com.multi.mlpenterpriseapprovalsystem.search.dto;

import lombok.*;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchDocument
 * @since : 2026. 1. 9. 금요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchDocument {
    private String docId;      // comId:TYPE:sourceId
    private String companyId;  // comId
    private String type;       // BOARD/SCHEDULE/APPROVAL
    private String sourceId;

    private String title;
    private String contentText;
    private String content;
    private String summary;

    private String url;

    private Long createdAt;
    private Long updatedAt;

    private List<String> acl; // 권한 토큰 (MVP는 ["COMP:COA"] 정도로 시작 가능)

    private String scope;
}