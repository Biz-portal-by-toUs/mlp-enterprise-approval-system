package com.multi.mlpenterpriseapprovalsystem.document_form.form;

import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.*;
import jakarta.persistence.*;

/**
 * 스탯 코드를 문자로 치환
 *
 * @author : 정종원
 * @filename : DocumentFormStatsConverter
 * @since : 2025-12-30 화요일
 */

@Converter(autoApply = false)
public class DocumentFormStatsConverter
        implements AttributeConverter<DocumentFormStats, String> {

    @Override
    public String convertToDatabaseColumn(DocumentFormStats attribute) {
        return attribute == null ? null : attribute.name(); // 항상 1글자
    }

    @Override
    public DocumentFormStats convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DocumentFormStats.valueOf(dbData);
    }
}
