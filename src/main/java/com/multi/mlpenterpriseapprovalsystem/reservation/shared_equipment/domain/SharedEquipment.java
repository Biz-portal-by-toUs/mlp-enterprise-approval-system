package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공유 설비 엔티티
 *
 * @author : 고송현
 * @filename : SharedEquipment
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "shared_equipment")
public class SharedEquipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long eqNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    @Column(length = 20, nullable = false)
    private String eqName;

    @Column(length = 20)
    private String eqId;

    @Column(length = 20)
    private String modelName;

    @Column(length = 255)
    private String imgUrl;

    @Column(length = 20)
    private String loc;
}