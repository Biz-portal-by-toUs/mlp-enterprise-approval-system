package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentForm;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempResDocumentFormDto
 * @since : 25. 12. 22. 월요일
 */
@Data
@Builder
public class TempResDocumentFormDto {
    private Long docfoNo;
    private String docfoName;
    private String cnttJson;
    private String cnttHtml;
    private List<TempResDocumentFormCategoryDto> tempResDocumentFormCategoryDtos;

    public static TempResDocumentFormDto toDto(DocumentForm documentForm) {
        return TempResDocumentFormDto.builder()
                .docfoNo(documentForm.getDocfoNo())
                .docfoName(documentForm.getDocfoName())
                .cnttJson(documentForm.getCnttJson())
                .cnttHtml(documentForm.getCnttHtml())
                .build();
    }
}
