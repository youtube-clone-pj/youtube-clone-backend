package com.youtube.notification.testfixtures.builder;

import com.youtube.core.user.domain.User;
import com.youtube.notification.domain.Notification;
import com.youtube.notification.domain.NotificationTargetType;
import com.youtube.notification.domain.NotificationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.Instant;

@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationBuilder {

    private Long id;
    private User receiver;
    private NotificationType notificationType = NotificationType.LIVE_STREAMING_STARTED;
    private NotificationTargetType targetType = NotificationTargetType.LIVE_STREAMING;
    private Long targetId = 1L;
    private String title = "테스트 알림 타이틀";
    private String thumbnailUrl = "https://example.com/thumbnail.jpg";
    private String deeplinkUrl = "/live/1";
    private boolean isRead = false;
    private Instant readAt;

    public static NotificationBuilder Notification() {
        return new NotificationBuilder();
    }

    public Notification build() {
        return Notification.builder()
                .id(this.id)
                .receiver(this.receiver)
                .notificationType(this.notificationType)
                .targetType(this.targetType)
                .targetId(this.targetId)
                .title(this.title)
                .thumbnailUrl(this.thumbnailUrl)
                .deeplinkUrl(this.deeplinkUrl)
                .isRead(this.isRead)
                .readAt(this.readAt)
                .build();
    }
}
