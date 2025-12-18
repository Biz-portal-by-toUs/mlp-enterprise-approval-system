package com.multi.mlpenterpriseapprovalsystem.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 수단 타입
 * C: 카드, A: 계좌
 * 사용예시:
 * PaymType cardType = PaymType.CARD;
 *
 * @author : 이지헌
 * @filename : CardType
 * @since : 25. 12. 16. 화요일
 */
@Getter
@RequiredArgsConstructor
public enum PaymType {
    C, // 카드
    A  // 계좌
}
