package com.multi.mlpenterpriseapprovalsystem.notice.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeReqDto;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : Notice
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice")
@AllArgsConstructor // 빌더 사용하려면 전체 생성자 필요
@Builder
public class Notice extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long noticeNo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;
    private Boolean isDeleted;
    private String title;
    @Column(columnDefinition = "json")
    private String contents;
    private Boolean isPopup;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "emp_id", referencedColumnName = "emp_id") private Employee employee;
    private Integer rating;

    public void update(@Valid NoticeReqDto dto) {

        this.isDeleted = dto.getIsDeleted();
        this.title = dto.getTitle();
        this.contents = dto.getContents();
        this.isPopup = dto.getIsPopup();
        this.startedAt = dto.getStartedAt();
        this.endedAt = dto.getEndedAt();

    }
}