package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentFormCategory;
import lombok.Builder;
import lombok.Data;

/**
 * 임시 문서양식 내 카테고리 응답 Dto
 *
 * @author : 이지헌
 * @filename : TempResDocumentFormCategoryDto
 * @since : 25. 12. 22. 월요일
 */
@Data
@Builder
public class TempResDocumentFormCategoryDto {
    private Long docfoCatNo;
    private String docfoCatName;

    public static TempResDocumentFormCategoryDto toDto(DocumentFormCategory documentFormCategory) {
        return TempResDocumentFormCategoryDto.builder()
                .docfoCatName(documentFormCategory.getName())
                .docfoCatNo(documentFormCategory.getDocfoCatNo())
                .build();
    }
}
