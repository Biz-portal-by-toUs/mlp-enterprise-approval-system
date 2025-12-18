package com.multi.mlpenterpriseapprovalsystem.auth.repository;

import com.multi.mlpenterpriseapprovalsystem.auth.domain.RefreshToken;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * refreshtoken repository
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
}