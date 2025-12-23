package com.multi.mlpenterpriseapprovalsystem.document.dto.req;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : ReqDocumentDto
 * @since : 25. 12. 20. 토요일
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReqDocumentDto {
    private String comId;

    private String writerId;
    private String writerName;
    private String writerDepName;
    private String writerWorkPhone;

    private Long docNo;
    private String docId;
    private String docStat;
    private String myApprStat;

    private String title;
    private String content;
    private String cnttHtml;
    private String aiSumm;
    private Boolean temp;

    private Long docfoNo;

    private Long docfoCatNo;
    private String docfoCatName;

    private LocalDateTime submittedAt;

    private List<ReqApprovalLineDto> approvalLines;
}
