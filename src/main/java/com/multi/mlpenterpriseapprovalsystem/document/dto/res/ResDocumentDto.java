package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 문서 반환 Dto
 *
 * @author : 이지헌
 * @filename : ResDocumentDto
 * @since : 25. 12. 17. 수요일
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResDocumentDto {
    private Long docNo;
    private DocStat docStat;
    private String docId;
    private String comId;
    private Long docFormNo;
    private String docFormName;
    private String docFormCatName;
    private String title;
    private String content;
    private List<ResApprovalLineDto> resApprovalLineDtos;
    private String cnttHtml;
    private String writer;
    private String writerId;
    private String aiSumm;
    private Boolean temp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ApprStat myApprStat; // 나의 결재 상태


    public static ResDocumentDto toDto(Document document) {
        return ResDocumentDto.builder()
                .docNo(document.getDocNo())
                .docStat(document.getDocStat())
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
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }


    public static ResDocumentDto toDto(Document document, String empId) {
        ResDocumentDto resDocumentDto = ResDocumentDto.builder()
                .docNo(document.getDocNo())
                .docStat(document.getDocStat())
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
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();

        // empId가 전달되었다면 해당 사용자의 상태를 찾아 세팅
        if (empId != null) {
            for (ApprovalLine line : document.getApprovalLines()) {
                if (line.getApprover().getEmpId().equals(empId)) {
                    resDocumentDto.setMyApprStat(line.getApprStat());
                    break;
                }
            }
        }

        return resDocumentDto;
    }
}
