package com.multi.mlpenterpriseapprovalsystem.search.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : ViewSearchController
 * @since : 2026. 1. 9. 금요일
 */
@Controller
@RequestMapping("/search")
public class ViewSearchController {

    @GetMapping
    public String getView(){
        return "search/search";
    }

}
