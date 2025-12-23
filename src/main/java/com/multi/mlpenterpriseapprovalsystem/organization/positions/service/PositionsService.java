package com.multi.mlpenterpriseapprovalsystem.organization.positions.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.dto.ReqPositionsDto;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.dto.ResPositionsDto;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.repository.PositionsRepository;
import jakarta.validation.Valid;
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
@Transactional
public class PositionsService {

    private final PositionsRepository positionsRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<ResPositionsDto> getPositions(String comId) {

        // 1) 직급 목록
        List<Positions> positions = positionsRepository.findAllByCompany_ComIdOrderByPosOrderAsc(comId);
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
                        .posOrder(p.getPosOrder())
                        .empCount(countMap.getOrDefault(p.getPosNo(), 0L))
                        .build())
                .toList();
    }

    public void addPositions(String comId, @Valid ReqPositionsDto reqPositionsDto) {

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 부서 이름이 이미 존재하는 경우
        if(positionsRepository.existsByCompanyAndPosName(company, reqPositionsDto.getPosName())) {

            throw new CustomException(ErrorCode.DUPLICATE_DEPNAME);
        }

        Positions positions = Positions.of(company, reqPositionsDto.getPosName().trim(), reqPositionsDto.getPosOrder());
        positionsRepository.save(positions);
    }

    public void updatePositions(String comId, Long posNo, @Valid ReqPositionsDto reqPositionsDto) {


        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));
        // 1) 내 회사 부서인지 확인 + 조회
        Positions positions = positionsRepository.findById(posNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));



        // 2) depId 변경 시 중복 체크 (같은 부서가 자기 depId 그대로면 OK)
        String newPosName = reqPositionsDto.getPosName();
        if (!positions.getPosName().equals(newPosName)) {
            boolean exists = positionsRepository.existsByCompanyAndPosName(company, newPosName);
            if (exists) {
                throw new CustomException(ErrorCode.DUPLICATE_POSNAME);
            }
        }

        // 3) 반영
        positions.update(newPosName, reqPositionsDto.getPosOrder());
        // JPA dirty checking으로 save() 없어도 됨
    }

    public void deletePositions(String comId, Long posNo) {

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 부서 존재 체크
        Positions positions = positionsRepository.findById(posNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // 다른 회사 부서 삭제 방지 (중요)
        if (!positions.getCompany().getComNo().equals(company.getComNo())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 부서에 사원 남아있으면 삭제 불가
        boolean hasEmployees = employeeRepository.existsByPositions_posNo(posNo);
        if (hasEmployees) {
            throw new CustomException(ErrorCode.POSITIONS_DELETE_HAS_EMPLOYEES);
        }

        positionsRepository.delete(positions);

    }
}
