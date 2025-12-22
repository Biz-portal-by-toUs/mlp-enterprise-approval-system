package com.multi.mlpenterpriseapprovalsystem.document.service;

import com.multi.mlpenterpriseapprovalsystem.document.dto.res.TempResDepartmentDto;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempDepartmentRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempDepartmentService
 * @since : 25. 12. 20. 토요일
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TempDepartmentService {

    private final TempDepartmentRepository tempDepartmentRepository;

    @Transactional(readOnly = true)
    public List<TempResDepartmentDto> findAllByDepName(String comId, String depName) {
        List<Department> departments = tempDepartmentRepository.findAllByDepNameAndCompany_comId(comId, depName);

        return departments.stream()                // 1. 스트림 생성
                .map(TempResDepartmentDto::toDto)  // 2. DTO로 변환
                .toList();                         // 3. 리스트로 반환 (Java 16+)
    }
}
