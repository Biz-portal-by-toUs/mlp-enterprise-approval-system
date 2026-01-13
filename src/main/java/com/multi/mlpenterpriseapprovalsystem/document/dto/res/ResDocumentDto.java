package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 문서 응답 Dto
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
    private Boolean resubmittedForTemp; // 재상신된 대상 문서의 임시저장 여부
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
                .resubmittedForTemp(document.getResubmittedFor() != null ?
                        document.getResubmittedFor().getTemp() : null)
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
                .resubmittedForTemp(document.getResubmittedFor() != null ?
                        document.getResubmittedFor().getTemp() : null)
                .resubmittedByDocNo(document.getResubmittedBy() != null ?
                        document.getResubmittedBy().getDocNo() : null)
                .build();

        // 2. ✅ 나의 대표 결재 상태 결정 (다중 순번 대응)
        if (empId != null && document.getApprovalLines() != null) {
            // 내 사번이 포함된 모든 결재 라인의 상태 추출
            List<ApprStat> myStats = document.getApprovalLines().stream()
                    .filter(line -> line.getApprover().getEmpId().equals(empId))
                    .map(ApprovalLine::getApprStat)
                    .toList();

            // 우선순위 결정: I(진행중) > W(대기중) > R(반려) > A(승인)
            ApprStat representativeStat = null;
            if (myStats.contains(ApprStat.I)) {
                representativeStat = ApprStat.I;
            } else if (myStats.contains(ApprStat.W)) {
                representativeStat = ApprStat.W;
            } else if (myStats.contains(ApprStat.R)) {
                representativeStat = ApprStat.R;
            } else if (myStats.contains(ApprStat.A)) {
                representativeStat = ApprStat.A;
            }

            resDocumentDto.setMyApprStat(representativeStat);
        }

        return resDocumentDto;
    }

    public static ResDocumentDto toDto(Document document, String empId, String contextStatus) {
        ResDocumentDto dto = toDto(document, empId); // 기존 기본 매핑 로직 호출 (빌더 부분)

        if (empId != null && document.getApprovalLines() != null) {
            List<ApprStat> myStats = document.getApprovalLines().stream()
                    .filter(line -> line.getApprover().getEmpId().equals(empId))
                    .map(ApprovalLine::getApprStat)
                    .toList();

            ApprStat representativeStat = null;

            // ✅ 상황에 따른 우선순위 결정
            if ("PROCESSED".equals(contextStatus)) {
                // 결재한 문서 목록에서는 승인(A)이나 반려(R)를 우선 표시
                representativeStat = myStats.contains(ApprStat.R) ? ApprStat.R :
                        (myStats.contains(ApprStat.A) ? ApprStat.A : null);
            } else if ("AWAITING".equals(contextStatus)) {
                // 결재할 문서 목록에서는 내 순서(I)나 대기(W)를 우선 표시
                representativeStat = myStats.contains(ApprStat.I) ? ApprStat.I :
                        (myStats.contains(ApprStat.W) ? ApprStat.W : null);
            }

            // 만약 위 조건으로 못 찾았다면 전체 우선순위(I > W > R > A) 적용
            if (representativeStat == null) {
                if (myStats.contains(ApprStat.I)) representativeStat = ApprStat.I;
                else if (myStats.contains(ApprStat.W)) representativeStat = ApprStat.W;
                else if (myStats.contains(ApprStat.R)) representativeStat = ApprStat.R;
                else if (myStats.contains(ApprStat.A)) representativeStat = ApprStat.A;
            }

            dto.setMyApprStat(representativeStat);
        }
        return dto;
    }
}
