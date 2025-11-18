package com.youtube.core.subscription.service;

import com.youtube.core.subscription.domain.SubscriptionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionQueryService {

    private final SubscriptionReader subscriptionReader;

    public boolean isSubscribed(final Long subscriberId, final Long channelId) {
        return subscriptionReader.existsBy(subscriberId, channelId);
    }
}
