package com.multi.mlpenterpriseapprovalsystem.subscription.service;

import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.dto.res.ResSubscriptionDto;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 요금제 관리 서비스
 *
 * @author : 이지헌
 * @filename : SubscriptionService
 * @since : 25. 12. 16. 화요일
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    // 전체 요금제 조회
    public List<ResSubscriptionDto> getSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();

        return subscriptions.stream()
                .map(subscription -> ResSubscriptionDto.builder()
                        .subNo(subscription.getSubNo())
                        .subName(subscription.getSubName())
                        .subPrice(subscription.getSubPrice())
                        .subDesc(subscription.getSubDesc())
                        .subLimit(subscription.getSubLimit())
                        .build())
                .toList();
    }
}
