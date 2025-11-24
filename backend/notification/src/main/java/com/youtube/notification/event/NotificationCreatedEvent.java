package com.youtube.notification.event;

import com.youtube.notification.domain.Notification;

public record NotificationCreatedEvent(
        Long notificationId,
        Long receiverId,
        String title,
        String thumbnailUrl,
        String deeplinkUrl,
        String targetType
) {
    public static NotificationCreatedEvent from(final Notification notification) {
        return new NotificationCreatedEvent(
                notification.getId(),
                notification.getReceiver().getId(),
                notification.getTitle(),
                notification.getThumbnailUrl(),
                notification.getDeeplinkUrl(),
                notification.getTargetType().name()
        );
    }
}
