package com.multi.mlpenterpriseapprovalsystem.organization.positions.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : Positions
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "positions")
public class Positions {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long posNo;

    @Column(nullable = false, length = 10)
    private String posName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId", nullable = false)
    private Company company;
}