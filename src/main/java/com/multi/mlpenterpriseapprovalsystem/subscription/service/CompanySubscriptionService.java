package com.multi.mlpenterpriseapprovalsystem.subscription.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.CompanySubscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.dto.response.ResCompanySubscriptionDto;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.CompanySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : CompanySubscriptionService
 * @since : 26. 1. 4. 일요일
 */

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CompanySubscriptionService {
    private final CompanySubscriptionRepository companySubscriptionRepository;

    public ResCompanySubscriptionDto getCurrentSubscription(String comId) {
        CompanySubscription sub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        Subscription pending = sub.getPendingSubscription();

        return ResCompanySubscriptionDto.builder()
                .subNo(sub.getSubscription().getSubNo())
                .subName(sub.getSubscription().getSubName())
                .status(sub.getStatus())
                .nextBillingDate(sub.getNextBillingDate())
                .autoRenewal(sub.isAutoRenewal())
                .pendingSubNo(pending != null ? pending.getSubNo() : null)
                .pendingSubName(pending != null ? pending.getSubName() : null)
                .empCnt(sub.getCompany().getEmpCnt())
                .pendingLimit(pending != null ? pending.getSubLimit() : 30)
                .creditBalance(sub.getCreditBalance())
                .build();
    }
}
