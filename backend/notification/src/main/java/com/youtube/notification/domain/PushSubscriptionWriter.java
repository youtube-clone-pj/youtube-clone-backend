package com.youtube.notification.domain;

import com.youtube.core.user.domain.User;
import com.youtube.notification.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushSubscriptionWriter {

    private final PushSubscriptionRepository pushSubscriptionRepository;

    @Transactional
    public PushSubscription subscribe(
            final User user,
            final String endpoint,
            final String p256dh,
            final String auth,
            final String userAgent
    ) {
        final PushSubscription pushSubscription = PushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh(p256dh)
                .auth(auth)
                .userAgent(userAgent)
                .build();

        final PushSubscription saved = pushSubscriptionRepository.save(pushSubscription);
        log.info("PushSubscription 등록 - userId: {}, endpoint: {}", user.getId(), endpoint);
        return saved;
    }

    @Transactional
    public void unsubscribe(final String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
        log.info("PushSubscription 해지 - endpoint: {}", endpoint);
    }

    @Transactional
    public void hardDelete(final Long pushSubscriptionId) {
        pushSubscriptionRepository.deleteById(pushSubscriptionId);
        log.info("PushSubscription 삭제 - pushSubscriptionId: {}", pushSubscriptionId);
    }

    @Transactional
    public void updateLastUsedDate(final Long pushSubscriptionId) {
        pushSubscriptionRepository.findById(pushSubscriptionId)
                .ifPresent(PushSubscription::updateLastUsedDate);
    }

    @Transactional
    public void activate(final Long pushSubscriptionId) {
        pushSubscriptionRepository.findById(pushSubscriptionId)
                .ifPresent(PushSubscription::activate);
    }

    @Transactional
    public void deactivate(final Long pushSubscriptionId) {
        pushSubscriptionRepository.findById(pushSubscriptionId)
                .ifPresent(PushSubscription::deactivate);
    }
}
