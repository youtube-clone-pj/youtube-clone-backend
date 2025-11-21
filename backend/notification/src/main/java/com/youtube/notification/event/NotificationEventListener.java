package com.youtube.notification.event;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.channel.domain.ChannelReader;
import com.youtube.core.subscription.domain.SubscriptionReader;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingReader;
import com.youtube.live.interaction.livestreaming.event.LiveStreamingStartedEvent;
import com.youtube.notification.domain.NotificationWriter;
import com.youtube.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SubscriptionReader subscriptionReader;
    private final ChannelReader channelReader;
    private final LiveStreamingReader liveStreamingReader;
    private final NotificationWriter notificationWriter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onLiveStreamingStarted(final LiveStreamingStartedEvent event) {
        log.info("라이브 스트리밍 시작 알림 생성 시작 - liveStreamingId: {}, channelId: {}",
                event.liveStreamingId(), event.channelId());

        try {
            final Channel channel = channelReader.readBy(event.channelId());
            final LiveStreaming liveStreaming = liveStreamingReader.readBy(event.liveStreamingId());

            final List<User> subscribers = subscriptionReader
                    .readSubscribersByChannelId(event.channelId());

            if (subscribers.isEmpty()) {
                log.info("구독자가 없어 알림 생성 생략 - channelId: {}", event.channelId());
                return;
            }

            final List<Notification> notifications = notificationWriter
                    .writeForLiveStreamingStart(subscribers, channel, liveStreaming);

            log.info("라이브 스트리밍 시작 알림 생성 완료 - 알림 수: {}, liveStreamingId: {}",
                    notifications.size(), event.liveStreamingId());

        } catch (Exception e) {
            log.warn("라이브 스트리밍 시작 알림 생성 실패 - liveStreamingId: {}, channelId: {}, error: {}",
                    event.liveStreamingId(), event.channelId(), e.getMessage(), e);
        }
    }
}
