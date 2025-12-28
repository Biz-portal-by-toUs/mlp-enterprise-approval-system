package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * 관리자용 사원 상세 DTO
 *
 * @author : 권지영
 * @filename : ResAdminEmployeeDetailDto
 * @since : 2025. 12. 28. 일요일
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ResAdminEmployeeDetailDto {

    // ===== 식별자 =====
    private Long empNo;
    private String empId;

    // ===== 회사 =====
    private String comId;
    private String comName;

    // ===== 부서 =====
    private Long depNo;
    private String depName;

    // ===== 직급 =====
    private Long posNo;
    private String posName;

    // ===== 사원 기본정보 =====
    private String empName;
    private String email;
    private String phone;
    private String workPhone;
    private String gen;
    private String addr;
    private LocalDate birth;
    private Integer age;

    // ===== 상태/권한 =====
    private RoleType role;
    private Boolean isDeleted;
    private String atte;
    private String msgStat;

    // ===== 재직 =====
    private LocalDate hireDate;
    private LocalDate retDate;

    // ===== 대직자 =====
    // - delegate FK가 emp_id라서 "수정 저장"에는 delegateEmpId가 핵심
    // - 화면(드롭다운 등)에서 선택/링크 편하게 하려고 empNo도 같이 내려줌
    private Long delegateEmpNo;     // ✅ 추가
    private String delegateEmpId;
    private String delegateName;

    // ===== 공통(BaseEntity) =====
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ResAdminEmployeeDetailDto from(Employee e) {
        if (e == null) return null;

        Employee del = e.getDelegate();
        Company c = e.getCompany();

        return ResAdminEmployeeDetailDto.builder()
                // 식별자
                .empNo(e.getEmpNo())
                .empId(e.getEmpId())

                // 회사
                .comId(c != null ? c.getComId() : null)
                .comName(c != null ? c.getComName() : null)

                // 부서
                .depNo(e.getDepartment() != null ? e.getDepartment().getDepNo() : null)
                .depName(e.getDepartment() != null ? e.getDepartment().getDepName() : null)

                // 직급
                .posNo(e.getPositions() != null ? e.getPositions().getPosNo() : null)
                .posName(e.getPositions() != null ? e.getPositions().getPosName() : null)

                // 기본정보
                .empName(e.getEmpName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .workPhone(e.getWorkPhone())
                .gen(e.getGen())
                .addr(e.getAddr())
                .birth(e.getBirth())
                .age(calcAge(e.getBirth()))

                // 상태/권한
                .role(e.getRole())
                .isDeleted(e.getIsDeleted())
                .atte(e.getAtte())
                .msgStat(e.getMsgStat())

                // 재직
                .hireDate(e.getHireDate())
                .retDate(e.getRetDate())

                // 대직자
                .delegateEmpNo(del != null ? del.getEmpNo() : null)   // ✅ 추가
                .delegateEmpId(del != null ? del.getEmpId() : null)
                .delegateName(del != null ? del.getEmpName() : null)

                // BaseEntity
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private static int calcAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
