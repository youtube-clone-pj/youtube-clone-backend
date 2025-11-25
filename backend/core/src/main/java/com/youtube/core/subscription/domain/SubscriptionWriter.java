package com.youtube.core.subscription.domain;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.subscription.repository.SubscriptionRepository;
import com.youtube.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionWriter {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionReader subscriptionReader;

    public Subscription write(final Subscription subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void remove(final Subscription subscription, final Instant deletedDate) {
        subscription.softDelete(deletedDate);
        log.info("Subscription 삭제 - subscriberId: {}, channelId: {}",
                subscription.getSubscriber().getId(), subscription.getChannel().getId());
    }

    @Transactional
    public boolean restore(final Long subscriberId, final Long channelId) {
        return subscriptionReader.readDeletedBy(subscriberId, channelId)
                .map(subscription -> {
                    subscription.restore();
                    log.info("삭제된 Subscription 복원 - subscriberId: {}, channelId: {}",
                            subscriberId, channelId);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void subscribe(final User subscriber, final Channel channel) {
        SubscriptionPolicy.validateNotSelfSubscription(subscriber.getId(), channel);

        final Optional<Subscription> existingSubscription = subscriptionReader.readBy(
                subscriber.getId(),
                channel.getId()
        );

        if (existingSubscription.isPresent()) {
            log.info("이미 구독 중인 채널 - subscriberId: {}, channelId: {}",
                    subscriber.getId(), channel.getId());
            return;
        }

        if (restore(subscriber.getId(), channel.getId())) {
            return;
        }

        createSubscription(subscriber, channel);
    }

    private void createSubscription(final User subscriber, final Channel channel) {
        write(Subscription.builder()
                .subscriber(subscriber)
                .channel(channel)
                .build()
        );
        log.info("Subscription 생성 - subscriberId: {}, channelId: {}",
                subscriber.getId(), channel.getId());
    }

    @Transactional
    public void unsubscribe(final Long subscriberId, final Long channelId, final Instant deletedDate) {
        remove(subscriptionReader.readByOrThrow(subscriberId, channelId), deletedDate);
    }
}
