package com.multi.mlpenterpriseapprovalsystem.common.api.nts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 국세청 사업자 상태 조회 API용 DTO
 * 
 * @filename    : NtsStatusDto
 * @author      : 권지영
 * @since       : 2025. 12. 19. 금요일
 */
public class NtsStatusDto {

    @Getter @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private List<String> b_no; // 사업자번호 목록 (배열 형태)
    }

    @Getter @Setter
    public static class Response {
        private String status_code;
        private Integer request_cnt;
        private Integer match_cnt;
        private List<Data> data;

        @Getter
        @Setter
        public static class Data {
            private String b_no;       // 사업자번호
            private String b_stt;      // 납세자상태 (계속사업자/휴업자/폐업자)
            private String b_stt_cd;   // 상태코드 (01/02/03)
            private String tax_type;   // 과세유형 (가장 중요: "국세청에 등록되지 않은..." 메시지 체크용)
            private String end_dt;     // 폐업일
        }
    }
}
