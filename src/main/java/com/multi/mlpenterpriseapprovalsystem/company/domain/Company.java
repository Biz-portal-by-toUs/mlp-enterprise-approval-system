package com.multi.mlpenterpriseapprovalsystem.company.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
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
@Table(name = "company", indexes = @Index(name = "idx_company_com_id", columnList = "com_id"))
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="com_no")
    private Long comNo;

    @Column(name = "com_id", nullable = false, unique = true, length = 3)
    private String comId; // 비즈니스 키

    @Column(name = "com_name", nullable = false, length = 50)
    private String comName;

    // [수정] 이메일 유니크 제약조건 추가
    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false)
    private String pwd;

    // [수정] int -> String(10) 변경
    @Column(nullable = false, length = 10)
    private String brn;

    @Column(name = "emp_cnt")
    private int empCnt;
    private String addr;

    // [추가] 이미지 URL 및 경로
    @Column(name = "img_url")
    private String imgUrl;
    private String path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_no")
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable=false, length=20)
    private RoleType role; // SYS_ADMIN or COM_ADMIN

    // Company 엔티티 안에 추가
    public static Company createForSignup(
            String comId, String comName, String email, String encodedPwd, String brn, String addr,
            String imgUrl, String path, Subscription subscription, RoleType role
    ) {
        Company c = new Company();
        c.comId = comId;
        c.comName = comName;
        c.email = email;
        c.pwd = encodedPwd;
        c.brn = brn;
        c.addr = addr;
        c.imgUrl = imgUrl;
        c.path = path;
        c.subscription = subscription;
        c.role = role;
        return c;
    }

    public void changePassword(String encodedPwd) {
        this.pwd = encodedPwd;
    }
}