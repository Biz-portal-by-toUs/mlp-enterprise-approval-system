package com.multi.mlpenterpriseapprovalsystem.common.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TokenDto : 토큰 정보가 담김
 *
 * @author : 권지영
 * @filename : TokenDto
 * @since : 2025. 12. 17. 수요일
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {
    @Builder.Default
    private String grantType = "Bearer";
    private String accessToken;
    private String refreshToken;
}
