package com.multi.mlpenterpriseapprovalsystem.cloud.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * FolderScope ↔ DB 컬럼(String) 변환을 담당하는 JPA 컨버터
 *
 * 왜 필요한가?
 *  - 기본적으로 JPA는 enum을
 *      ORDINAL(숫자) 또는 NAME(문자열)
 *    로 저장한다.
 *
 *  - 하지만 우리는 FolderScope 내부의
 *      code 값("dept", "prvt")
 *    을 저장하고 사용하고 싶다.
 *
 * @author : 송현님
 * @filename : FolderScopeConverter
 * @since : 2025-12-30 오전 11:12 화요일
 */

@Converter(autoApply = false)
public class FolderScopeConverter implements AttributeConverter<FolderScope, String> {

    @Override
    public String convertToDatabaseColumn(FolderScope attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public FolderScope convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FolderScope.from(dbData);
    }
}
