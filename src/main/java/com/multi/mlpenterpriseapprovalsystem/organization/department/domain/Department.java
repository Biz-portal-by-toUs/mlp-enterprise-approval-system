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
// [중요] 부서 ID는 회사(com_id) 내에서만 유니크합니다. (전역 유니크 아님)
@Table(name = "department", uniqueConstraints = @UniqueConstraint(columnNames = {"com_id", "dep_id"}))
public class Department {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long depNo;

    // [주의] unique = true를 붙이지 않습니다.
    @Column(nullable = false, length = 3)
    private String depId;

    @Column(nullable = false, length = 10)
    private String depName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id", nullable = false)
    private Company company;
}