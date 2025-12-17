package com.multi.mlpenterpriseapprovalsystem.mail.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : MailUserState
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mail_user_state", uniqueConstraints = @UniqueConstraint(columnNames = {"mail_id", "user_id"}))
public class MailUserState extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recipientNo;

    // Mail의 mailId(UK) 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_id", referencedColumnName = "mail_id", nullable = false)
    private Mail mail;

    // Employee의 empId(UK) 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "emp_id", nullable = false)
    private Employee user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MailRole role;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isPrior = false;

    private LocalDateTime deletedAt;
    private LocalDateTime purgedAt;
}