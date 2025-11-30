package com.youtube.notification.domain;

import com.youtube.notification.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PushSubscriptionReader {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    public List<PushSubscription> readAllByUserId(final Long userId) {
        return pushSubscriptionRepository.findAllByUserId(userId);
    }

    public List<PushSubscription> readAllByUserIdAndActive(final Long userId, final boolean active) {
        return pushSubscriptionRepository.findAllByUserIdAndActive(userId, active);
    }

    public Optional<PushSubscription> readByEndpoint(final String endpoint) {
        return pushSubscriptionRepository.findByEndpoint(endpoint);
    }
}