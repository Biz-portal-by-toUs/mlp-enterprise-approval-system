package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentFormCategory;
import lombok.Builder;
import lombok.Data;

/**
 * 임시 문서양식 내 카테고리 응답 Dto
 *
 * @author : 이지헌
 * @filename : ResDocumentFormCategoryDtoV2
 * @since : 25. 12. 22. 월요일
 */
@Data
@Builder
public class ResDocumentFormCategoryDtoV2 {
    private Long docfoCatNo;
    private String docfoCatName;

    public static ResDocumentFormCategoryDtoV2 toDto(DocumentFormCategory documentFormCategory) {
        return ResDocumentFormCategoryDtoV2.builder()
                .docfoCatName(documentFormCategory.getName())
                .docfoCatNo(documentFormCategory.getDocfoCatNo())
                .build();
    }
}
