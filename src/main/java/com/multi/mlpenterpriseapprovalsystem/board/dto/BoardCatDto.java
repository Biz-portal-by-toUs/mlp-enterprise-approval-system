package com.multi.mlpenterpriseapprovalsystem.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : BoardCatDto
 * @since : 2025-12-19 금요일
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardCatDto {
    private Long boardCatNo;
    private Character catCode;
    private String catDescript;
}
