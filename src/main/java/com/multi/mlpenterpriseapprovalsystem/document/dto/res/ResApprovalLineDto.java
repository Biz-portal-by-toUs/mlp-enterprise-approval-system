package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제라인 응답 Dto
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
    private Long docNo;
    private String docId;
    private String approverName; // 결재자 이름
    private String approverId; // 결재자 사원번호
    private String approverDepName;
    private String approverPosName;
    private int approverPosOrder;
    private String comId;
    private int seq;
    private ApprStat apprStat;
    private LocalDateTime endedAt;
    @JsonProperty("isActualAppr")
    private boolean isActualAppr;
    private String rejReason;
    @JsonProperty("isDelegate")
    private boolean isDelegate;

    // ✅ 추가: 누구를 대신해서 결재했는지 표시할 이름
    private String targetApproverName;

    public static ResApprovalLineDto toDto(ApprovalLine approvalLine) {
        return ResApprovalLineDto.builder()
                .apprlNo(approvalLine.getApprlNo())
                .docNo(approvalLine.getDocument().getDocNo())
                .docId(approvalLine.getDocument().getDocId())
                .approverName(approvalLine.getApprover().getEmpName())
                .approverId(approvalLine.getApprover().getEmpId())
                .approverDepName(approvalLine.getApprover().getDepartment().getDepName())
                .approverPosName(approvalLine.getApprover().getPositions().getPosName())
                .approverPosOrder(approvalLine.getApprover().getPositions().getPosOrder())
                .comId(approvalLine.getCompany().getComId())
                .seq(approvalLine.getSeq())
                .apprStat(approvalLine.getApprStat())
                .endedAt(approvalLine.getEndedAt())
                .isActualAppr(approvalLine.getIsActualAppr())
                .rejReason(approvalLine.getRejReason())
                // ✅ 추가: targetApprover가 존재할 경우 이름 매핑
                .targetApproverName(approvalLine.getTargetApprover() != null ?
                        approvalLine.getTargetApprover().getEmpName() : null)
                .isDelegate(approvalLine.getIsDelegate())
                .build();
    }

}
