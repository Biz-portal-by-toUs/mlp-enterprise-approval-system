package com.multi.mlpenterpriseapprovalsystem.search.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : MenuConfig
 * @since : 2026. 1. 9. 금요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MenuConfig {
    private List<Menu> menus = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Menu {
        private String menuKey;       // "A"
        private String title;         // "공지사항 · 자료실"
        private List<String> types;   // ["BOARD"]
        private int size = 5;
        private int order = 1;
    }
}