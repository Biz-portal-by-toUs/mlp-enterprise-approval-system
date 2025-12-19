package com.multi.mlpenterpriseapprovalsystem.reservation.share_equipment.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SharedEquipment
 * @since : 2025. 12. 16. 화요일
 */
@Entity @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "shared_equipment")
public class SharedEquipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long eqNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    private String eqName;
    private String eqId;
    private String modelName;
    private String imgUrl;
    private String loc;
}