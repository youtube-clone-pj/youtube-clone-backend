package com.youtube.notification.service;

import com.youtube.core.user.domain.User;
import com.youtube.core.user.domain.UserReader;
import com.youtube.notification.domain.PushSubscription;
import com.youtube.notification.domain.PushSubscriptionReader;
import com.youtube.notification.domain.PushSubscriptionWriter;
import com.youtube.notification.service.dto.PushSubscribeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionWriter pushSubscriptionWriter;
    private final PushSubscriptionReader pushSubscriptionReader;
    private final UserReader userReader;

    @Transactional
    public void subscribe(final Long userId, final PushSubscribeRequest request) {
        final User user = userReader.readBy(userId);

        pushSubscriptionReader.readByEndpoint(request.endpoint())
                .ifPresent(pushSubscription ->
                        pushSubscriptionWriter.hardDelete(pushSubscription.getId())
                );

        pushSubscriptionWriter.subscribe(
                user,
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth(),
                request.userAgent()
        );
    }

    @Transactional
    public void unsubscribe(final String endpoint) {
        pushSubscriptionReader.readByEndpoint(endpoint)
                .ifPresent(pushSubscription ->
                    pushSubscriptionWriter.unsubscribe(endpoint)
                );
    }

    @Transactional
    public void deactivateAllSubscriptions(final Long userId) {
        final List<PushSubscription> subscriptions = pushSubscriptionReader.readAllByUserId(userId);

        subscriptions.forEach(subscription ->
                pushSubscriptionWriter.deactivate(subscription.getId())
        );
    }

    @Transactional
    public void reactivateSubscription(final Long userId, final String endpoint) {
        pushSubscriptionReader.readByEndpoint(endpoint)
                .filter(subscription -> subscription.getUser().getId().equals(userId))
                .ifPresent(subscription ->
                        pushSubscriptionWriter.activate(subscription.getId())
                );
    }
}
