package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.*;

/**
 * 법인 차량 엔티티
 *
 * @author : 고송현
 * @filename : CorporateCar
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "corporate_car")
@Builder
public class CorporateCar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long carNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    @Column(length = 20, nullable = false)
    private String carName;

    @Column(length = 20)
    private String carType;

    @Column(length = 20, nullable = false)
    private String plateNo;

    @Column(nullable = false)
    private Integer cap;

    @Column(length = 10)
    private String fuel;

    @Column(length = 255)
    private String imgUrl;
}