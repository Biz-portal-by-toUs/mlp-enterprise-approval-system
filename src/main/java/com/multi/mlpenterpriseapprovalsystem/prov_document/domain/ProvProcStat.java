package com.multi.mlpenterpriseapprovalsystem.prov_document.domain;

/**
 * 임베딩 상태 ENUM
 * 
 * @filename    : ProvProcStat
 * @author      : 김승기
 * @since       : 2025. 12. 29. 월요일
 */
public enum ProvProcStat {
    CREATED,
    UPLOADED,
    PROCESSING,
    DONE,
    FAILED
}