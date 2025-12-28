package com.multi.mlpenterpriseapprovalsystem.employee.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.*;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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

    public ResEmployeeDetailDto getEmployeeDetailById(String myEmpId) {
        Employee employee = employeeRepository.findByEmpId(myEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        return ResEmployeeDetailDto.from(employee);
    }

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
        LocalDateTime now = LocalDateTime.now();

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
}