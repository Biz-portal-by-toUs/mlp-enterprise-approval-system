package com.multi.mlpenterpriseapprovalsystem.document.dto.req;

import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentForm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class ReqDocumentDto {
    private String comId;

    private String writerId;
    private String writerName;
    private String writerDepName;

    private Long docNo;
    private String docId;
    private String docStat;
    private String myApprStat;

    private String title;
    private String content;
    private String cnttHtml;
    private String aiSumm;
    private Boolean temp;

    private DocumentForm docfoNo;

    private Long docfoCatNo;
    private String docfoCatName;

    private List<ApprovalLine> approvalLines;
}
