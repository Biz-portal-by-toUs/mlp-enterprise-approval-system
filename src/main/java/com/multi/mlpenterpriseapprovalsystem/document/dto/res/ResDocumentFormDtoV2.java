package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentForm;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 임시 문서양식 응답 Dto
 *
 * @author : 이지헌
 * @filename : ResDocumentFormDtoV2
 * @since : 25. 12. 22. 월요일
 */
@Data
@Builder
public class ResDocumentFormDtoV2 {
    private Long docfoNo;
    private String docfoName;
    private String cnttJson;
    private String cnttHtml;
    private List<ResDocumentFormCategoryDtoV2> resDocumentFormCategoryDtoV2s;

    public static ResDocumentFormDtoV2 toDto(DocumentForm documentForm) {
        return ResDocumentFormDtoV2.builder()
                .docfoNo(documentForm.getDocfoNo())
                .docfoName(documentForm.getDocfoName())
                .cnttJson(documentForm.getCnttJson())
                .cnttHtml(documentForm.getCnttHtml())
                .build();
    }
}
