package com.youtube.core.subscription.domain;

import com.youtube.core.channel.domain.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionPolicy {

    public static void validateNotSelfSubscription(final Long userId, final Channel channel) {
        if (channel.isOwnedBy(userId)) {
            throw new IllegalArgumentException("자기 자신의 채널은 구독할 수 없습니다.");
        }
    }
}