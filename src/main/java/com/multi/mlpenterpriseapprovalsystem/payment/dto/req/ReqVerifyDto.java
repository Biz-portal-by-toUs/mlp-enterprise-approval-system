package com.multi.mlpenterpriseapprovalsystem.payment.dto.req;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 브라우저에서 카드등록하면, 프론트가 이 서버에 전달하는 빌링키 관련 데이터
 * 프론트가 전달했기 때문에 유효성 검증할 필요 있음
 *
 * @author : 이지헌
 * @filename : VerifyRequestDto
 * @since : 25. 12. 17. 수요일
 */
@Getter
@Setter
@ToString
public class ReqVerifyDto {
    private String impUid; // 포트원 결제ID
    private String customerUid; // 발급된 빌링키
    private Integer paidAmount; // 카드 등록이기 때문에 0원이 와야함
}
