package com.multi.mlpenterpriseapprovalsystem.payment.repository;

import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결제내역 테이블 관리 레포지토리
 *
 * @author : 이지헌
 * @filename : PaymentHistoryRepository
 * @since : 26. 1. 2. 금요일
 */
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    Page<PaymentHistory> findByCompany_ComIdOrderByCreatedAtDesc(String comId, Pageable pageable);
}
