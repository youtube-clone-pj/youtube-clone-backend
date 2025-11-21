package com.youtube.core.subscription.domain;

import com.youtube.core.subscription.repository.SubscriptionRepository;
import com.youtube.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SubscriptionReader {

    private final SubscriptionRepository subscriptionRepository;

    public Optional<Subscription> readBy(final Long subscriberId, final Long channelId) {
        return subscriptionRepository.findBySubscriberIdAndChannelId(subscriberId, channelId);
    }

    public Subscription readByOrThrow(final Long subscriberId, final Long channelId) {
        return subscriptionRepository.findBySubscriberIdAndChannelId(subscriberId, channelId)
                .orElseThrow(() -> new IllegalArgumentException("구독하지 않은 채널입니다"));
    }

    public Optional<Subscription> readDeletedBy(final Long subscriberId, final Long channelId) {
        return subscriptionRepository.findDeletedBySubscriberIdAndChannelId(subscriberId, channelId);
    }

    public boolean existsBy(final Long subscriberId, final Long channelId) {
        return subscriptionRepository.existsBySubscriberIdAndChannelId(subscriberId, channelId);
    }

    public List<User> readSubscribersByChannelId(final Long channelId) {
        return subscriptionRepository.findSubscribersByChannelId(channelId);
    }
}
