package com.multi.mlpenterpriseapprovalsystem.notice.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : NoticeAttach
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_attach")
public class NoticeAttach {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long noticeAttachNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "comId") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "notice_no") private Notice notice;
    private String orgName;
    private String folderPath;
}