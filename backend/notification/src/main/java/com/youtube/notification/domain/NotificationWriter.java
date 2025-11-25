package com.youtube.notification.domain;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWriter {

    private final NotificationRepository notificationRepository;

    public Notification write(final Notification notification) {
        return notificationRepository.save(notification);
    }

    public List<Notification> writeForLiveStreamingStart(
            final List<User> subscribers,
            final Channel channel,
            final LiveStreaming liveStreaming
    ) {
        final String title = String.format(
                "%s 실시간 스트리밍 중: %s",
                channel.getChannelName(),
                liveStreaming.getTitle()
        );
        final String deeplinkUrl = String.format("/lives/%d", liveStreaming.getId());

        final List<Notification> notifications = subscribers.stream()
                .map(subscriber -> Notification.builder()
                        .receiver(subscriber)
                        .notificationType(NotificationType.LIVE_STREAMING_STARTED)
                        .targetType(NotificationTargetType.LIVE_STREAMING)
                        .targetId(liveStreaming.getId())
                        .title(title)
                        .thumbnailUrl(liveStreaming.getThumbnailUrl())
                        .deeplinkUrl(deeplinkUrl)
                        .isRead(false)
                        .build())
                .toList();

        //TODO bulk insert 고려
        final List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

        log.info("알림 생성 완료 - 알림 수: {}, 수신자 수: {}, 알림 타입: {}",
                savedNotifications.size(), subscribers.size(), NotificationType.LIVE_STREAMING_STARTED);

        return savedNotifications;
    }

    @Transactional
    public long markAllAsRead(final Long userId) {
        final List<Notification> unreadNotifications = notificationRepository
                .findUnreadByReceiverId(userId);

        unreadNotifications.forEach(Notification::markAsRead);

        log.info("모든 알림 읽음 처리 완료 - userId: {}, 처리된 알림 수: {}", userId, unreadNotifications.size());
        return unreadNotifications.size();
    }
}
