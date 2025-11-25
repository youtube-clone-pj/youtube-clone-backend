package com.youtube.api.notification;

import com.youtube.notification.event.NotificationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseEmitterManager {

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분

    /**
     * 사용자별 SSE 연결 목록
     * Key: userId, Value: 해당 사용자의 SseEmitter 집합 (여러 탭/브라우저 지원)
     */
    private final ConcurrentHashMap<Long, Set<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter createConnection(final Long userId) {
        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(throwable -> {
            log.warn("SSE 연결 에러 - userId: {}, error: {}", userId, throwable.getMessage());
            removeEmitter(userId, emitter);
        });

        emittersByUserId.compute(userId, (key, emitters) -> {
            if (emitters == null) {
                emitters = ConcurrentHashMap.newKeySet();
            }
            emitters.add(emitter);
            return emitters;
        });

        sendInitialEvent(userId, emitter);

        log.info("SSE 연결 생성 - userId: {}", userId);
        return emitter;
    }

    public void sendNotification(final Long userId, final NotificationCreatedEvent event) {
        final Set<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        emitters.forEach(emitter -> send(emitter, userId, event));
    }

    public boolean hasConnection(final Long userId) {
        final Set<SseEmitter> emitters = emittersByUserId.get(userId);
        return emitters != null && !emitters.isEmpty();
    }

    public void sendUnreadCount(final Long userId, final long unreadCount) {
        final Set<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        emitters.forEach(emitter -> sendUnreadCountToEmitter(emitter, userId, unreadCount));
    }

    private void removeEmitter(final Long userId, final SseEmitter emitter) {
        emittersByUserId.computeIfPresent(userId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    private void sendInitialEvent(final Long userId, final SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 연결 성공"));
        } catch (IOException e) {
            log.warn("SSE 초기 연결 이벤트 전송 실패 - userId: {}", userId);
            removeEmitter(userId, emitter);
            throw new IllegalStateException("SSE 초기 연결 실패", e);
        }
    }

    private void send(final SseEmitter emitter, final Long userId, final NotificationCreatedEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(event));
        } catch (IOException e) {
            log.warn("SSE 알림 전송 실패 - userId: {}, notificationId: {}", userId, event.notificationId());
            removeEmitter(userId, emitter);
        }
    }

    private void sendUnreadCountToEmitter(final SseEmitter emitter, final Long userId, final long unreadCount) {
        try {
            emitter.send(SseEmitter.event()
                    .name("unread-count")
                    .data(unreadCount));
        } catch (IOException e) {
            log.warn("SSE 읽지 않은 개수 전송 실패 - userId: {}", userId);
            removeEmitter(userId, emitter);
        }
    }
}
