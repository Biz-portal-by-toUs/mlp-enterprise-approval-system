package com.multi.mlpenterpriseapprovalsystem.subscription.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.math.BigDecimal;

/**
 * 응답용 SubscriptionDto
 *
 * @author : 이지헌
 * @filename : SubscriptionDto
 * @since : 25. 12. 16. 화요일
 */

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonPropertyOrder({"subNo", "subName", "subDesc", "subPrice"})
public class ResSubscriptionDto {
    private Integer subNo;
    private String subName;
    private String subDesc;
    private BigDecimal subPrice;
    private Integer subLimit;
}
