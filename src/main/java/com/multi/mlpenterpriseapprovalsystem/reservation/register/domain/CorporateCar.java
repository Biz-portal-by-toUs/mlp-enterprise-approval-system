package com.multi.mlpenterpriseapprovalsystem.reservation.register.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : CorporateCar
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "corporate_car")
public class CorporateCar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long carNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "comId") private Company company;
    private String carName;
    private String carType;
    private String plateNo;
    private Integer cap;
    private String fuel;
    private String imgUrl;
}