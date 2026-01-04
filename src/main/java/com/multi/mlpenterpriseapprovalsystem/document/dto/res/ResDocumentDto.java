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
    private Long docFormCatNo;
    private String title;
    private String content;
    private List<ResApprovalLineDto> resApprovalLineDtos;
    private String cnttHtml;
    private String writer;
    private String writerId;
    private String writerDepName;
    private String writerDepId;
    private Long writerDepNo;
    private String writerWorkPhone;
    private String aiSumm;
    private Boolean temp;
    private LocalDateTime createdAt; // 상신일
    private LocalDateTime updatedAt;
    private ApprStat myApprStat; // 나의 결재 상태
    private LocalDateTime submittedAt;

    private Boolean isResubmitted;
    private Long resubmittedForDocNo;
    private Long resubmittedByDocNo;


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
                .writerDepName(document.getWriter().getDepartment().getDepName())
                .writerDepNo(document.getWriter().getDepartment().getDepNo())
                .writerDepId(document.getWriter().getDepartment().getDepId())
                .writerWorkPhone(document.getWriter().getWorkPhone())
                .aiSumm(document.getAiSumm())
                .temp(document.getTemp())
                .docFormName(document.getDocumentForm().getDocfoName())
                .docFormNo(document.getDocumentForm().getDocfoNo())
                .docFormCatName(document.getDocumentFormCategory().getName())
                .docFormCatNo(document.getDocumentFormCategory().getDocfoCatNo())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .submittedAt(document.getSubmittedAt())
                .isResubmitted(document.getIsResubmitted())
                .resubmittedForDocNo(document.getResubmittedFor() != null ?
                        document.getResubmittedFor().getDocNo() : null)
                .resubmittedByDocNo(document.getResubmittedBy() != null ?
                        document.getResubmittedBy().getDocNo() : null)
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
                .writerDepName(document.getWriter().getDepartment().getDepName())
                .writerDepNo(document.getWriter().getDepartment().getDepNo())
                .writerDepId(document.getWriter().getDepartment().getDepId())
                .writerWorkPhone(document.getWriter().getWorkPhone())
                .aiSumm(document.getAiSumm())
                .temp(document.getTemp())
                .docFormName(document.getDocumentForm().getDocfoName())
                .docFormNo(document.getDocumentForm().getDocfoNo())
                .docFormCatName(document.getDocumentFormCategory().getName())
                .docFormCatNo(document.getDocumentFormCategory().getDocfoCatNo())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .submittedAt(document.getSubmittedAt())
                .isResubmitted(document.getIsResubmitted())
                .resubmittedForDocNo(document.getResubmittedFor() != null ?
                        document.getResubmittedFor().getDocNo() : null)
                .resubmittedByDocNo(document.getResubmittedBy() != null ?
                        document.getResubmittedBy().getDocNo() : null)
                .build();

        // ✅ 수정된 로직: 본인이 원결재자이거나, 원결재자의 현재 대직자인 경우 상태 추출
        if (empId != null && document.getApprovalLines() != null) {
            for (ApprovalLine line : document.getApprovalLines()) {
                // 1. 내가 원결재자인지 확인
                boolean isApprover = line.getApprover().getEmpId().equals(empId);

                // 2. 내가 원결재자의 현재 대직자인지 확인 (Employee 테이블의 관계 확인)
                boolean isCurrentDelegate = line.getApprover().getDelegate() != null &&
                        line.getApprover().getDelegate().getEmpId().equals(empId);

                if (isApprover || isCurrentDelegate) {
                    // 원결재자든 대직자든, 해당 결재 순번의 상태(I/W/A/R)를 내 상태로 취급
                    resDocumentDto.setMyApprStat(line.getApprStat());
                    break;
                }
            }
        }

        return resDocumentDto;
    }
}
