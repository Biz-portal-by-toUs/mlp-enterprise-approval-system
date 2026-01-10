package com.multi.mlpenterpriseapprovalsystem.document.dto.req;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : ReqApprovalLineDto
 * @since : 25. 12. 20. 토요일
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReqApprovalLineDto {
    private Long apprlNo;
    private String docNo;
    private String approverName; // 결재자 이름
    private String approverId; // 결재자 사원번호
    private String comId;
    private Integer seq;
    private String apprStat;
    private LocalDateTime endedAt;
    private Boolean isActualAppr;

    @Size(max = 200, message = "반려이유는 최대 200자까지 입력 가능합니다.")
    private String rejReason;
    private Boolean isDelegate;
}
