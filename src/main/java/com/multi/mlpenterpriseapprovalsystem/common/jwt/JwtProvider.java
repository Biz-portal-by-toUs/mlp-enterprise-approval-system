package com.multi.mlpenterpriseapprovalsystem.common.jwt;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * jwtProvider
 *
 * @author : 권지영
 * @filename : JwtProvider
 * @since : 2025. 12. 17. 수요일
 */
@Component
public class JwtProvider {

    @Getter
    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret}")
    private String secretKey;

    public SecretKey getSecretKey() {
        // Secret Key Base64 디코딩
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(decodedKey);
    }


}
