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
    @JoinColumn(name = "com_id", referencedColumnName = "com_id", nullable = false)
    private Company company;

    private Positions(Company company, String posName) {
        this.company = company;
        this.posName = posName;
    }

    public static Positions of(Company company, String posName) {
        return new Positions(company, posName);
    } // 이 형식으로 넣어야지만 생성이 가능하게
}