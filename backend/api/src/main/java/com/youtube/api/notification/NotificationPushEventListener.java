package com.youtube.api.notification;

import com.youtube.notification.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushEventListener {

    private final SseEmitterManager sseEmitterManager;

    @Async
    @EventListener
    public void onNotificationCreated(final NotificationCreatedEvent event) {
        final Long receiverId = event.receiverId();

        try {
            // SSE 연결이 있으면 SSE로 전송
            if (sseEmitterManager.hasConnection(receiverId)) {
                sseEmitterManager.sendNotification(receiverId, event);
                log.info("알림 전송 성공 (SSE) - notificationId: {}, receiverId: {}",
                        event.notificationId(), receiverId);
            } else {
                // TODO: WebPush 전송 구현 예정
                log.debug("SSE 연결 없음, WebPush 전송 예정 - notificationId: {}, receiverId: {}",
                        event.notificationId(), receiverId);
            }
        } catch (Exception e) {
            log.warn("알림 푸시 전송 실패 - notificationId: {}, receiverId: {}, error: {}",
                    event.notificationId(), receiverId, e.getMessage(), e);
        }
    }
}
