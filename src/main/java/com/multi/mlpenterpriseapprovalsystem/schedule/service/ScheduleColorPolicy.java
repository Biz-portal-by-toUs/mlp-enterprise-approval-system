package com.multi.mlpenterpriseapprovalsystem.schedule.service;

import com.multi.mlpenterpriseapprovalsystem.schedule.enums.CalendarScope;

/**
 * 색 정책 클래스
 *
 * @author : 권지영
 * @filename : ScheduleColorPolicy
 * @since : 2025. 12. 31. 수요일
 */
public final class ScheduleColorPolicy {

    private ScheduleColorPolicy() {}

    public static final String COMPANY_COLOR = "#2563EB";
    public static final String DEPARTMENT_COLOR = "#16A34A";
    public static final String PERSONAL_DEFAULT = "#f59e0b";



    public static String resolve(CalendarScope scope, String requestedColor) {
        if (scope == CalendarScope.COMPANY) return COMPANY_COLOR;
        if (scope == CalendarScope.DEPARTMENT) return DEPARTMENT_COLOR;

        return PERSONAL_DEFAULT;
    }
}
