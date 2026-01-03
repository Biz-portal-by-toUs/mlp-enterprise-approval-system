package com.multi.mlpenterpriseapprovalsystem.payment.repository;

import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결제내역 레포지토리
 *
 * @author : 이지헌
 * @filename : PaymentHistoryRepository
 * @since : 26. 1. 2. 금요일
 */
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
}
