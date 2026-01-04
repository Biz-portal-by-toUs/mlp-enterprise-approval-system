package com.multi.mlpenterpriseapprovalsystem.employee.enums;

/**
 * 메시지 상태 enum
 * 
 * @filename    : MsgStat
 * @author      : 권지영
 * @since       : 2026. 1. 4. 일요일
 */
public enum MsgStat {
    WORKING('C', "근무 중", "dot-green", true),
    MEETING('M', "회의 중", "dot-gray", true),
    FOCUS('D', "업무 집중", "dot-yellow", true),
    AWAY('X', "자리 비움", "dot-red", true),
    OFF('H', "출근 안함", "dot-gray", false); // 로그인 상태에선 선택 불가

    private final char code;
    private final String label;
    private final String cssDotClass;
    private final boolean selectable;

    MsgStat(char code, String label, String cssDotClass, boolean selectable) {
        this.code = code;
        this.label = label;
        this.cssDotClass = cssDotClass;
        this.selectable = selectable;
    }

    public char getCode() { return code; }
    public String getLabel() { return label; }
    public String getCssDotClass() { return cssDotClass; }
    public boolean isSelectable() { return selectable; }

    public static MsgStat fromCode(char code) {
        for (MsgStat s : values()) if (s.code == code) return s;
        throw new IllegalArgumentException("Unknown msgStat: " + code);
    }
}
