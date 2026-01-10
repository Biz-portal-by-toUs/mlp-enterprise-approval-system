package com.multi.mlpenterpriseapprovalsystem.auth.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refresh_token 엔티티
 *
 * @author : 김승기
 * @filename : RefreshToken
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "refresh_token",
        indexes = @Index(
                name = "idx_refresh_token_subject",
                columnList = "subject_type, subject_id"
        )
)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 10)
    private TokenSubjectType subjectType; // EMPLOYEE / COMPANY

    @Column(name = "subject_id", nullable = false)
    private Long subjectId; // EMPLOYEE.emp_no / COMPANY.com_no

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;


    @Builder
    public RefreshToken(TokenSubjectType subjectType, Long subjectId, String token,
                        LocalDateTime expiredAt) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.token = token;
        this.expiredAt = expiredAt;
        this.revoked = false;
    }

    public void revoke() {
        this.revoked = true;
    }
}