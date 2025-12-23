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

    @Column(name = "pos_order")
    private Integer posOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id", nullable = false)
    private Company company;

    private Positions(Company company, String posName, Integer posOrder) {
        this.company = company;
        this.posName = posName;
        this.posOrder = posOrder;
    }

    public static Positions of(Company company, String posName, Integer posOrder) {
        return new Positions(company, posName, posOrder);
    }

    public void update(String posName, Integer posOrder) {
        this.posName = posName;
        this.posOrder = posOrder;
    }
}