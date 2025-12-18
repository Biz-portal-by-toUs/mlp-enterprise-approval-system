package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : ResDocumentDto
 * @since : 25. 12. 17. 수요일
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResDocumentDto {
    private Long docNo;
    private String comId;
    private String docId;
    private String docFormCatName;
    private String title;
    private String content;
    private List<ResApprovalLineDto> resApprovalLineDtos;
    private String cnttHtml;
    private String writer;
    private String writerId;
    private String aiSumm;
    private Boolean temp;
    private String docFormName;
    private Long docFormNo;


    public static ResDocumentDto toDto(Document document) {
        return ResDocumentDto.builder()
                .docNo(document.getDocNo())
                .comId(document.getCompany().getComId())
                .docId(document.getDocId())
                .docFormCatName(document.getDocumentFormCategory().getName())
                .title(document.getTitle())
                .content(document.getContent())
                .resApprovalLineDtos(document.getApprovalLines().stream()
                        .map(ResApprovalLineDto::toDto)
                        .toList())
                .cnttHtml(document.getCnttHtml())
                .writer(document.getWriter().getEmpName())
                .writerId(document.getWriter().getEmpId())
                .aiSumm(document.getAiSumm())
                .temp(document.getTemp())
                .docFormName(document.getDocumentForm().getDocfoName())
                .docFormNo(document.getDocumentForm().getDocfoNo())
                .build();
    }
}
