package com.multi.mlpenterpriseapprovalsystem.cloud.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * 클라우드 폴더의 사용 범위를 나타내는 enum
 *
 * @author : 송현님
 * @filename : FolderScope
 * @since : 2025-12-30 오전 11:08 화요일
 */
public enum FolderScope {
    DEPT("dept"),
    PRVT("prvt");

    private final String code;

    FolderScope(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static FolderScope from(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(e -> e.code.equals(v) || e.name().equalsIgnoreCase(v))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("scope는 dept/prvt만 가능합니다: " + value));
    }
}
