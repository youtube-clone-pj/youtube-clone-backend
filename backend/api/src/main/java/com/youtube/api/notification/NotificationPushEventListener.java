package com.youtube.api.notification;

import com.youtube.notification.domain.NotificationReader;
import com.youtube.notification.event.NotificationCreatedEvent;
import com.youtube.notification.service.WebPushPayloadConverter;
import com.youtube.notification.service.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPushEventListener {

    private final NotificationSseManager notificationSseManager;
    private final NotificationReader notificationReader;
    private final WebPushService webPushService;
    private final WebPushPayloadConverter webPushPayloadConverter;

    @Async
    @EventListener
    public void onNotificationCreated(final NotificationCreatedEvent event) {
        final Long receiverId = event.receiverId();

        if (notificationSseManager.hasConnection(receiverId)) {
            notificationSseManager.sendNotification(receiverId, event);
            notificationSseManager.sendUnreadCount(receiverId, notificationReader.countUnreadBy(receiverId));
        } else {
            webPushService.sendNotification(receiverId, webPushPayloadConverter.toPayload(event));
        }
    }
}
