package com.multi.mlpenterpriseapprovalsystem.auth.repository;

import com.multi.mlpenterpriseapprovalsystem.auth.domain.RefreshToken;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * refresh token db 접근 레포지토리
 *
 * @author : 권지영
 * @filename : RefreshTokenRepository
 * @since : 2025. 12. 17. 수요일
 */

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findTopBySubjectTypeAndSubjectIdAndRevokedFalseOrderByRefNoDesc(
            TokenSubjectType subjectType,
            Long subjectId
    );

    List<RefreshToken> findAllBySubjectTypeAndSubjectIdAndRevokedFalse(TokenSubjectType subjectType, Long subjectId);
}