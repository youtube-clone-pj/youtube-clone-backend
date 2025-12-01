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
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onLiveStreamingStarted(final LiveStreamingStartedEvent event) {
        log.info("LiveStreaming 시작 이벤트 수신 - liveStreamingId: {}, channelId: {}",
                event.liveStreamingId(), event.channelId());

        try {
            final Channel channel = channelReader.readBy(event.channelId());
            final LiveStreaming liveStreaming = liveStreamingReader.readBy(event.liveStreamingId());

            final List<User> subscribers = subscriptionReader
                    .readSubscribersByChannelId(event.channelId());

            if (subscribers.isEmpty()) {
                log.info("구독자가 없어 Notification 미생성 - liveStreamingId: {}, channelId: {}",
                        event.liveStreamingId(), event.channelId());
                return;
            }

            final List<Notification> notifications = notificationWriter
                    .writeForLiveStreamingStart(subscribers, channel, liveStreaming);

            // 생성된 알림에 대해 NotificationCreatedEvent 발행
            notifications.forEach(notification -> {
                eventPublisher.publishEvent(NotificationCreatedEvent.from(notification));
            });

            log.info("LiveStreaming 시작 Notification 생성 및 이벤트 발행 완료 - 알림 수: {}, liveStreamingId: {}, channelId: {}",
                    notifications.size(), event.liveStreamingId(), event.channelId());

        } catch (Exception e) {
            log.warn("LiveStreaming 시작 Notification 생성 실패 - liveStreamingId: {}, channelId: {}",
                    event.liveStreamingId(), event.channelId(), e);
        }
    }
}
