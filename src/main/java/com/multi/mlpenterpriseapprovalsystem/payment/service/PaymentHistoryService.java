package com.multi.mlpenterpriseapprovalsystem.payment.service;

import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentHistory;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.res.ResPaymentHistoryDto;
import com.multi.mlpenterpriseapprovalsystem.payment.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제내역 관리 서비스
 *
 * @author : 이지헌
 * @filename : PaymentHistoryService
 * @since : 26. 1. 8. 목요일
 */

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    public Page<ResPaymentHistoryDto> getPaymentHistories(String comId, Pageable pageable) {
        Page<PaymentHistory> historyPage = paymentHistoryRepository.findByCompany_ComIdOrderByCreatedAtDesc(comId, pageable);

        return historyPage.map(history -> {
            PaymentMethod method = history.getPaymentMethod();

            return ResPaymentHistoryDto.builder()
                    .payhNo(history.getPayhNo())
                    .amount(history.getAmount())
                    .payResult(history.getPayResult())
                    // DB에서 가져온 개별 필드를 그대로 매핑
                    .paymentMethodNo(method != null ? method.getPaymNo() : null)
                    .cardType(method != null ? method.getCardType() : "기타")
                    .mask(method != null ? method.getMask() : "****")
                    .createdAt(history.getCreatedAt())
                    .build();
        });
    }
}
