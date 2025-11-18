package com.youtube.core.testfixtures.builder;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.subscription.domain.Subscription;
import com.youtube.core.user.domain.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;


@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SubscriptionBuilder {

    private Long id;
    private User subscriber;
    private Channel channel;

    public static SubscriptionBuilder Subscription() {
        return new SubscriptionBuilder();
    }

    public Subscription build() {
        return Subscription.builder()
                .id(this.id)
                .subscriber(this.subscriber)
                .channel(this.channel)
                .build();
    }
}
