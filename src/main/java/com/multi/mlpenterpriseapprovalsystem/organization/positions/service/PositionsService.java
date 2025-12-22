package com.multi.mlpenterpriseapprovalsystem.organization.positions.service;

import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.dto.ResPositionsDto;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.repository.PositionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 직급 등록, 조회, 수정, 삭제 관련 서비스
 * 
 * @author : 권지영
 * @filename : PositionsService
 * @since : 2025. 12. 22. 월요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PositionsService {

    private final PositionsRepository positionsRepository;
    private final EmployeeRepository employeeRepository;

    public List<ResPositionsDto> getPositions(String comId) {

        // 1) 직급 목록
        List<Positions> positions = positionsRepository.findAllByCompany_ComId(comId);
        if (positions.isEmpty()) {
            return List.of();
        }

        // 2) 직급별 사원 수 (한 방 쿼리) - posNo 기준
        Map<Long, Long> countMap = employeeRepository.countGroupByPosNo(comId).stream()
                .collect(Collectors.toMap(
                        EmployeeRepository.PosCount::getPosNo,
                        EmployeeRepository.PosCount::getCnt
                ));

        // 3) DTO 조립
        return positions.stream()
                .map(p -> ResPositionsDto.builder()
                        .posName(p.getPosName())
                        .posNo(p.getPosNo())
                        .empCount(countMap.getOrDefault(p.getPosNo(), 0L))
                        .build())
                .toList();
    }
}
