package com.youtube.core.subscription.service;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.channel.domain.ChannelReader;
import com.youtube.core.subscription.domain.SubscriptionWriter;
import com.youtube.core.user.domain.User;
import com.youtube.core.user.domain.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionWriter subscriptionWriter;
    private final UserReader userReader;
    private final ChannelReader channelReader;

    @Transactional
    public void subscribe(final Long userId, final Long channelId) {
        final User subscriber = userReader.readBy(userId);
        final Channel channel = channelReader.readBy(channelId);

        subscriptionWriter.subscribe(subscriber, channel);
    }

    @Transactional
    public void unsubscribe(final Long userId, final Long channelId, final Instant deletedDate) {
        subscriptionWriter.unsubscribe(userId, channelId, deletedDate);
    }
}
