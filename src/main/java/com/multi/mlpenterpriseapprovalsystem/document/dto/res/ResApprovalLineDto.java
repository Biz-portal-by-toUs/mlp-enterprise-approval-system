package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제라인 반환 Dto
 *
 * @author : 이지헌
 * @filename : ResApprovalLineDto
 * @since : 25. 12. 18. 목요일
 */

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResApprovalLineDto {
    private Long apprlNo;
    private String docNo;
    private String approverName; // 결재자 이름
    private String approverId; // 결재자 사원번호
    private String comId;
    private int seq;
    private ApprStat apprStat;
    private LocalDateTime endedAt;

    public static ResApprovalLineDto toDto(ApprovalLine approvalLine) {
        return ResApprovalLineDto.builder()
                .apprlNo(approvalLine.getApprlNo())
                .docNo(approvalLine.getDocument().getDocId())
                .approverName(approvalLine.getApprover().getEmpName())
                .approverId(approvalLine.getApprover().getEmpId())
                .comId(approvalLine.getCompany().getComId())
                .seq(approvalLine.getSeq())
                .apprStat(approvalLine.getApprStat())
                .endedAt(approvalLine.getEndedAt())
                .build();
    }

}
