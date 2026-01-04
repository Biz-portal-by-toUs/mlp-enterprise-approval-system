package com.multi.mlpenterpriseapprovalsystem.employee.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.*;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.department.repository.DepartmentRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.repository.PositionsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

/**
 * 사원 관련 서비스
 *
 * @author : 김승기
 * @filename : EmployeeService
 * @since : 2025. 12. 20. 토요일
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionsRepository positionsRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ✅ 이름순 정렬 + 검색 + 커서 기반 무한스크롤
     * cursor는 String(불투명 커서)로 받음
     */
    @Transactional(readOnly = true)
    public ResChatEmployeeCursorDto getEmployeesByCursor(String requesterEmpId,
                                                 String keyword,
                                                 String cursor,
                                                 int size,
                                                 boolean excludeMe) {

        String comId = employeeRepository.findCompanyIdByEmpId(requesterEmpId);
        if (comId == null) {
            throw new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }

        String excludeEmpId = excludeMe ? requesterEmpId : null;


        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pr = PageRequest.of(0, safeSize + 1);

        CursorKey ck = decodeCursor(cursor); // cursorName + cursorNo

        List<ChatEmployeeItemDto> rows = employeeRepository.findChatEmployeesByNameCursor(
                comId,
                excludeEmpId,
                keyword,
                ck.name,
                ck.no,
                pr
        );

        boolean hasNext = rows.size() > safeSize;
        if (hasNext) {
            rows = rows.subList(0, safeSize);
        }

        String nextCursor = null;
        if (!rows.isEmpty()) {
            ChatEmployeeItemDto last = rows.get(rows.size() - 1);
            nextCursor = encodeCursor(last.getEmpName(), last.getEmpNo());
        }

        return new ResChatEmployeeCursorDto(rows, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public ResEmployeeDetailDto getEmployeeDetailById(String myEmpId) {
        Employee employee = employeeRepository.findByEmpId(myEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        return ResEmployeeDetailDto.from(employee);
    }

    @Transactional(readOnly = true)
    public List<ResEmployeeListDto> searchEmployees(String comId, Long depNo, Long posNo, Boolean isDeleted, String keyword) {
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return employeeRepository.searchEmployees(comId, depNo, posNo, isDeleted, searchKeyword);
    }

    @Transactional(readOnly = true)
    public ResAdminEmployeeDetailDto getEmployeeDetail(String comId, Long empNo) {

        Employee employee = employeeRepository.findAdminDetailByComIdAndEmpNo(comId, empNo)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        return ResAdminEmployeeDetailDto.from(employee);
    }

    public void retireEmployee(String comId, Long empNo) {
        LocalDate now = LocalDate.now();

        int updated = employeeRepository.retireEmployee(comId, empNo, now);
        if (updated == 1) return; // 정상적으로 퇴사 처리됨

        // updated==0 이면: (1) 사원이 없음 or (2) 이미 퇴사 상태
        Employee e = employeeRepository.findByEmpNoAndCompany_ComId(empNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND)); // 너희 프로젝트에 맞는 NOT_FOUND로 바꿔도 됨

        if (Boolean.TRUE.equals(e.getIsDeleted())) {
            // 이미 퇴사: 에러로 처리할지, 그냥 성공으로 볼지 선택
            throw new CustomException(ErrorCode.EMPLOYEE_ALREADY_RETIRED); // 예: "이미 퇴사 처리된 사원" 같은 코드로 바꾸면 더 좋음
        }

        // 이론상 여기까지 잘 안 옴(동시성 등). 그래도 안전빵:
        throw new CustomException(ErrorCode.EMPLOYEE_ALREADY_RETIRED);
    }

    public ResAdminEmployeeCreateDto createEmployee(String comId, ReqAdminEmployeeCreateDto req) {

        // ✅ 회사 row 락(동시등록 시 empId 중복 방지)
        Company company = companyRepository.findByComIdForUpdate(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Department department = departmentRepository.findByCompanyAndDepNo(company, req.getDepNo())
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Positions positions = positionsRepository.findByCompanyAndPosNo(company, req.getPosNo())
                .orElseThrow(() -> new CustomException(ErrorCode.POSITIONS_NOT_FOUND));

        String nextEmpId = nextEmpId(comId);

        // ✅ pwd는 1234로 자동 저장(무조건 인코딩해서 저장 권장)
        String encodedPwd = passwordEncoder.encode("1234");

        Employee saved = employeeRepository.save(
                Employee.create(
                        nextEmpId,
                        company,
                        department,
                        positions,
                        encodedPwd,
                        req.getEmpName(),
                        req.getEmail(),
                        req.getPhone(),
                        req.getWorkPhone(),
                        req.getGen(),
                        req.getHireDate(),
                        req.getRole(),
                        req.getAddr(),
                        req.getObjectKey(),
                        req.getBirth()
                )
        );

        return new ResAdminEmployeeCreateDto(saved.getEmpNo(), saved.getEmpId(), "1234");
    }

    private String nextEmpId(String comId) {
        int maxNo = employeeRepository.findMaxEmpNoByComId(comId);
        int next = maxNo + 1;

        while (true) {
            String candidate = comId + String.format("%04d", next);
            if (!employeeRepository.existsByCompany_ComIdAndEmpId(comId, candidate)) {
                return candidate;
            }
            next++;
        }
    }

    public void updateObjectKey(String comId, Long empNo, String objectKey) {
        Employee emp = employeeRepository.findByEmpNoAndCompany_ComId(empNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        emp.updateObjectKey(objectKey); // 엔티티 메서드로 세팅
        // save() 안해도 dirty checking으로 업데이트됨
    }

    public void updateEmployee(String comId, Long empNo, @Valid ReqAdminEmployeeUpdateDto req) {

        Employee emp = employeeRepository
                .findByEmpNoAndCompany_ComId(empNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Company company = companyRepository.findByComIdForUpdate(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Department dep = departmentRepository.findByCompanyAndDepNo(company, req.getDepNo())
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

        Positions pos = positionsRepository.findByCompanyAndPosNo(company, req.getPosNo())
                .orElseThrow(() -> new CustomException(ErrorCode.POSITIONS_NOT_FOUND));

        emp.updateAdminInfo(
                dep, pos,
                req.getEmail(),
                req.getPhone(),
                req.getWorkPhone(),
                req.getAddr(),
                req.getGen(),   // "M" or "F"
                req.getRole()
        );
    }

    private record CursorKey(String name, Long no) {}

    /**
     * cursor 포맷: Base64Url( empName + "\n" + empNo )
     */
    private CursorKey decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new CursorKey(null, null);
        }
        try {
            byte[] raw = Base64.getUrlDecoder().decode(cursor);
            String decoded = new String(raw, StandardCharsets.UTF_8);

            int idx = decoded.lastIndexOf('\n');
            if (idx < 0) return new CursorKey(null, null);

            String name = decoded.substring(0, idx);
            Long no = Long.valueOf(decoded.substring(idx + 1));
            return new CursorKey(name, no);
        } catch (Exception e) {
            // 커서 깨졌으면 첫 페이지로 간주 or 예외 (여기선 예외 추천)
            throw new CustomException(ErrorCode.INVALID_CURSOR);
        }
    }

    private String encodeCursor(String name, Long empNo) {
        String payload = (name == null ? "" : name) + "\n" + empNo;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    // 마이페이지 정보 수정
    public void updateMyProfile(CustomUser user, ReqUpdateMyProfileDto req) {
        if (user == null || user.getSubjectId() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long empNo = user.getSubjectId();

        Employee emp = employeeRepository.findById(empNo)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // ✅ 방식 A: 프론트에서 항상 전체 값을 보내므로 null 방어 + trim만 해주고 비교
        String newEmail    = normalize(req.getEmail());
        String newPhone    = normalize(req.getPhone());
        String newWorkPhone= normalize(req.getWorkPhone());
        String newAddr     = normalize(req.getAddr());

        String curEmail     = normalize(emp.getEmail());
        String curPhone     = normalize(emp.getPhone());
        String curWorkPhone = normalize(emp.getWorkPhone());
        String curAddr      = normalize(emp.getAddr());

        // 1) email: 바뀐 경우에만 중복 체크 후 반영
        if (!newEmail.equals(curEmail)) {
            if (employeeRepository.existsByEmailAndEmpNoNot(newEmail, empNo)) {
                throw new CustomException(ErrorCode.DUPLICATE_EMAIL); // 너 프로젝트 에러코드로
            }
            emp.setEmail(newEmail); // setter 막혀있으면 changeEmail로 변경
        }

        // 2) phone
        if (!newPhone.equals(curPhone)) {
            emp.setPhone(newPhone);
        }

        // 3) workPhone
        if (!newWorkPhone.equals(curWorkPhone)) {
            emp.setWorkPhone(newWorkPhone);
        }

        // 4) addr
        if (!newAddr.equals(curAddr)) {
            emp.setAddr(newAddr);
        }

        // dirty checking으로 자동 UPDATE
    }

    private String normalize(String v) {
        return v == null ? "" : v.trim();
    }
}