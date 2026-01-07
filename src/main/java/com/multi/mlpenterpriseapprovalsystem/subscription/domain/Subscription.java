package com.multi.mlpenterpriseapprovalsystem.subscription.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 요금제 엔티티
 *
 * @filename    : Subscription
 * @author      : 김승기
 * @since       : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "subscription")
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer subNo;

    @Column(nullable = false, unique = true, length = 20)
    private String subName;

    private String subDesc;

    @Column(nullable = false, precision = 8)
    private BigDecimal subPrice;

    @Column(nullable = false)
    private Integer subLimit;
}