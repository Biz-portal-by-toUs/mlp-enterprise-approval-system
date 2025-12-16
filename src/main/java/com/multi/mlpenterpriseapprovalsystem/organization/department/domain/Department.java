package com.multi.mlpenterpriseapprovalsystem.organization.department.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : Department
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department", uniqueConstraints = @UniqueConstraint(columnNames = {"com_id", "dep_id"}))
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long depNo;

    @Column(nullable = false, unique = true, length = 3)
    private String depId;

    @Column(nullable = false, length = 10)
    private String depName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId", nullable = false)
    private Company company;
}