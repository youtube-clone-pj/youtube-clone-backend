package com.youtube.core.subscription.domain;

import com.youtube.common.exception.BaseException;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.subscription.exception.SubscriptionErrorCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionPolicy {

    public static void validateNotSelfSubscription(final Long userId, final Channel channel) {
        if (channel.isOwnedBy(userId)) {
            throw new BaseException(SubscriptionErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED);
        }
    }
}