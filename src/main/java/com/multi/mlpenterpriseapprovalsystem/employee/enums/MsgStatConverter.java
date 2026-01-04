package com.multi.mlpenterpriseapprovalsystem.employee.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * db에는 알파벳 한자리로 저장하게 바꿔주는 컨버터
 * 
 * @filename    : MsgStatConverter
 * @author      : 권지영
 * @since       : 2026. 1. 4. 일요일
 */
@Converter(autoApply = true)
public class MsgStatConverter implements AttributeConverter<MsgStat, String> {
    @Override
    public String convertToDatabaseColumn(MsgStat attribute) {
        return attribute == null ? null : String.valueOf(attribute.getCode());
    }

    @Override
    public MsgStat convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        return MsgStat.fromCode(dbData.charAt(0));
    }
}