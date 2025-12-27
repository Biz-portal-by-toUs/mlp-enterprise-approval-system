package com.multi.mlpenterpriseapprovalsystem.employee.repository;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ChatEmployeeItemDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ResEmployeeListDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Employee db 접근하는 레포지토리
 *
 * @author : 김승기
 * @filename : EmployeeRepository
 * @since : 2025. 12. 17. 수요일
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmpId(String empId);

    @Query("""
                select e.company.comId
                from Employee e
                where e.empId = :empId
            """)
    String findCompanyIdByEmpId(@Param("empId") String empId);

    /**
     * ✅ 이름순(카톡식) + 무한스크롤(커서)
     * 커서 조건:
     * - cursorName이 null이면 첫 페이지
     * - (empName > cursorName) OR (empName == cursorName AND empNo > cursorNo)
     */
    @Query("""
                select new com.multi.mlpenterpriseapprovalsystem.employee.dto.ChatEmployeeItemDto(
                    e.empNo,
                    e.empId,
                    e.empName,
                    d.depName,
                    p.posName,
                    e.msgStat,
                    e.atte,
                    e.email
                )
                from Employee e
                join e.department d
                join e.positions p
                where e.company.comId = :comId
                  and e.isDeleted = false
                  and (:excludeEmpId is null or e.empId <> :excludeEmpId)
                  and (
                      :keyword is null or :keyword = '' or
                      lower(e.empId)   like lower(concat('%', :keyword, '%')) or
                      lower(e.empName) like lower(concat('%', :keyword, '%')) or
                      lower(d.depName) like lower(concat('%', :keyword, '%')) or
                      lower(p.posName) like lower(concat('%', :keyword, '%'))
                  )
                  and (
                      :cursorName is null
                      or e.empName > :cursorName
                      or (e.empName = :cursorName and e.empNo > :cursorNo)
                  )
                order by e.empName asc, e.empNo asc
            """)
    List<ChatEmployeeItemDto> findChatEmployeesByNameCursor(
            @Param("comId") String comId,
            @Param("excludeEmpId") String excludeEmpId,
            @Param("keyword") String keyword,
            @Param("cursorName") String cursorName,
            @Param("cursorNo") Long cursorNo,
            Pageable pageable
    );

    boolean existsByDepartment_DepNo(Long depNo);

    boolean existsByPositions_posNo(Long posNo);

    @Query("SELECT new com.multi.mlpenterpriseapprovalsystem.employee.dto.ResEmployeeListDto(" +
            "e.empNo, e.empId, e.empName, d.depName, p.posName, e.role, e.isDeleted, e.email) " +
            "FROM Employee e " +
            "JOIN e.department d " +
            "JOIN e.positions p " +
            "WHERE e.company.comId = :comId " +
            "AND (:depNo IS NULL OR d.depNo = :depNo) " +
            "AND (:posNo IS NULL OR p.posNo = :posNo) " +
            "AND (:isDeleted IS NULL OR e.isDeleted = :isDeleted) " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "    OR e.empName LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY e.empNo DESC")
    List<ResEmployeeListDto> searchEmployees(
            @Param("comId") String comId,
            @Param("depNo") Long depNo,
            @Param("posNo") Long posNo,
            @Param("isDeleted") Boolean isDeleted,
            @Param("keyword") String keyword
    );

    List<Employee> findByEmpIdIn(List<String> attendeeIds);

    interface PosCount {
        Long getPosNo();
        Long getCnt();
    }

    @Query("""
                select p.posNo as posNo, count(e) as cnt
                from Employee e
                join e.company c
                join e.positions p
                where c.comId = :comId
                group by p.posNo
            """)
    List<PosCount> countGroupByPosNo(@Param("comId") String comId);

    interface DepCount {
        String getDepId();
        long getCnt();
    }

    @Query("""
                select e.department.depId as depId, count(e) as cnt
                from Employee e
                where e.company.comId = :comId
                group by e.department.depId
            """)
    List<DepCount> countGroupByDepId(@Param("comId") String comId);
}