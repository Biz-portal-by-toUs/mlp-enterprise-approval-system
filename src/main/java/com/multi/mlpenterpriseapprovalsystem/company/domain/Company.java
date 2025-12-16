package com.multi.mlpenterpriseapprovalsystem.company.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회사 엔티티
 *
 * @author : 김승기
 * @filename : Company
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company", indexes = @Index(name = "idx_company_com_id", columnList = "comId"))
public class Company extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long comNo;

    @Column(nullable = false, unique = true, length = 3)
    private String comId;

    @Column(nullable = false, length = 50)
    private String comName;

    @Column(nullable = false, unique = true, length = 50)
    private String email; // UK

    @Column(nullable = false)
    private String pwd;

    @Column(nullable = false, length = 10)
    private String brn;

    private int empCnt;
    private String addr;

    private String imgUrl;
    private String path;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_no")
    private Subscription subscription;
}