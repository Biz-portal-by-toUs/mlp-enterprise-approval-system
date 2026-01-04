package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums;

/**
 * 달력 보는 범위 (회사/부서/사원)
 *
 * @author : 권지영
 * @filename : CalendarScope
 * @since : 2025. 12. 30. 화요일
 */
public enum CalendarScope {
    PERSONAL,    // emp_schedule
    DEPARTMENT,  // schedule (dep_no = ?)
    COMPANY      // schedule (dep_no is null)
}
