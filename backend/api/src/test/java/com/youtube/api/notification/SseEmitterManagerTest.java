package com.youtube.api.notification;

import com.youtube.notification.event.NotificationCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SseEmitterManagerTest {

    private SseEmitterManager sut;

    @BeforeEach
    void setUp() {
        sut = new SseEmitterManager();
    }

    @Test
    @DisplayName("SSE 연결을 생성하면 해당 유저의 연결이 추가된다")
    void createConnectionAddsEmitterForUser() {
        // given
        final Long userId = 1L;

        // when
        sut.createConnection(userId);

        // then
        assertThat(sut.hasConnection(userId)).isTrue();
    }

    @Test
    @DisplayName("같은 유저에 대해 여러 연결을 생성하면 모두 유지된다")
    void createMultipleConnectionsForSameUser() throws Exception {
        // given
        final Long userId = 1L;

        // when
        final SseEmitter emitter1 = sut.createConnection(userId);
        final SseEmitter emitter2 = sut.createConnection(userId);

        // then
        assertThat(emitter1).isNotSameAs(emitter2);
        assertThat(sut.hasConnection(userId)).isTrue();
        assertThat(getEmittersForUser(userId)).hasSize(2);
    }

    @Test
    @DisplayName("연결이 없는 유저에게 알림을 전송해도 예외가 발생하지 않는다")
    void sendNotificationToUserWithoutConnectionDoesNotThrowException() {
        // given
        final Long userId = 1L;
        final NotificationCreatedEvent event = createTestEvent();

        // when & then
        assertThatCode(() -> sut.sendNotification(userId, event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("알림을 전송하면 해당 유저의 모든 연결에 알림이 전송된다")
    void sendNotificationToAllEmittersForUser() throws Exception {
        // given
        final Long userId = 1L;
        final NotificationCreatedEvent event = createTestEvent();

        final SseEmitter spyEmitter1 = spy(new SseEmitter());
        final SseEmitter spyEmitter2 = spy(new SseEmitter());

        // doNothing()은 메서드의 실제 동작만 막는 것이다. 메서드 호출 자체를 기록하는 것은 막지 않는다.
        doNothing().when(spyEmitter1).send(any(SseEmitter.SseEventBuilder.class));
        doNothing().when(spyEmitter2).send(any(SseEmitter.SseEventBuilder.class));

        addEmitterDirectly(userId, spyEmitter1);
        addEmitterDirectly(userId, spyEmitter2);

        // when
        sut.sendNotification(userId, event);

        // then
        verify(spyEmitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(spyEmitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("같은 사용자가 여러 브라우저에서 동시에 연결해도 모든 연결이 정확히 유지된다")
    void concurrentConnectionsFromSameUser() throws Exception {
        // given
        final Long userId = 1L;
        final int connectionCount = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(connectionCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(connectionCount);
        final List<SseEmitter> createdEmitters = new ArrayList<>();

        // when
        for (int i = 0; i < connectionCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    final SseEmitter emitter = sut.createConnection(userId);
                    synchronized (createdEmitters) {
                        createdEmitters.add(emitter);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        final boolean completed = endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(completed).isTrue();
        assertThat(createdEmitters).hasSize(connectionCount);
        assertThat(sut.hasConnection(userId)).isTrue();
        assertThat(getEmittersForUser(userId)).hasSize(connectionCount);
    }

    @Test
    @DisplayName("여러 사용자가 동시에 연결을 생성해도 각자의 연결이 정확히 관리된다")
    void concurrentConnectionsFromMultipleUsers() throws Exception {
        // given
        final int userCount = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(userCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(userCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 1; i <= userCount; i++) {
            final Long userId = (long) i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    sut.createConnection(userId);
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        final boolean completed = endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(userCount);

        for (long userId = 1; userId <= userCount; userId++) {
            assertThat(sut.hasConnection(userId)).isTrue();
            assertThat(getEmittersForUser(userId)).hasSize(1);
        }
    }

    @Test
    @DisplayName("동시에 여러 알림을 전송해도 모든 연결에 정확히 전달된다")
    void concurrentNotificationSending() throws Exception {
        // given
        final Long userId = 1L;
        final int emitterCount = 5;
        final int notificationCount = 10;

        final List<SseEmitter> mockEmitters = new ArrayList<>();
        for (int i = 0; i < emitterCount; i++) {
            final SseEmitter mockEmitter = spy(new SseEmitter());
            mockEmitters.add(mockEmitter);
            addEmitterDirectly(userId, mockEmitter);
        }

        final ExecutorService executor = Executors.newFixedThreadPool(notificationCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(notificationCount);

        // when
        for (int i = 0; i < notificationCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    final NotificationCreatedEvent event = createTestEvent();
                    sut.sendNotification(userId, event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        final boolean completed = endLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(completed).isTrue();
        for (SseEmitter emitter : mockEmitters) {
            verify(emitter, times(notificationCount)).send(any(SseEmitter.SseEventBuilder.class));
        }
    }

    @Test
    @DisplayName("연결이 없는 유저에게 읽지 않은 알림 개수를 전송해도 예외가 발생하지 않는다")
    void sendUnreadCountToUserWithoutConnectionDoesNotThrowException() {
        // given
        final Long userId = 1L;
        final long unreadCount = 5L;

        // when & then
        assertThatCode(() -> sut.sendUnreadCount(userId, unreadCount))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("읽지 않은 알림 개수를 전송하면 해당 유저의 모든 연결에 전달된다")
    void sendUnreadCountToAllEmittersForUser() throws Exception {
        // given
        final Long userId = 1L;
        final long unreadCount = 3L;

        final SseEmitter spyEmitter1 = spy(new SseEmitter());
        final SseEmitter spyEmitter2 = spy(new SseEmitter());

        doNothing().when(spyEmitter1).send(any(SseEmitter.SseEventBuilder.class));
        doNothing().when(spyEmitter2).send(any(SseEmitter.SseEventBuilder.class));

        addEmitterDirectly(userId, spyEmitter1);
        addEmitterDirectly(userId, spyEmitter2);

        // when
        sut.sendUnreadCount(userId, unreadCount);

        // then
        verify(spyEmitter1, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(spyEmitter2, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }
    
    private NotificationCreatedEvent createTestEvent() {
        return new NotificationCreatedEvent(
                1L,
                1L,
                "Test notification",
                "https://example.com/thumbnail.jpg",
                "https://example.com/deeplink",
                "TEST_TYPE"
        );
    }

    @SuppressWarnings("unchecked")
    private Set<SseEmitter> getEmittersForUser(final Long userId) throws Exception {
        final Field field = SseEmitterManager.class.getDeclaredField("emittersByUserId");
        field.setAccessible(true);
        final ConcurrentHashMap<Long, Set<SseEmitter>> emittersByUserId =
                (ConcurrentHashMap<Long, Set<SseEmitter>>) field.get(sut);
        return emittersByUserId.get(userId);
    }

    @SuppressWarnings("unchecked")
    private void addEmitterDirectly(final Long userId, final SseEmitter emitter) throws Exception {
        final Field field = SseEmitterManager.class.getDeclaredField("emittersByUserId");
        field.setAccessible(true);
        final ConcurrentHashMap<Long, Set<SseEmitter>> emittersByUserId =
                (ConcurrentHashMap<Long, Set<SseEmitter>>) field.get(sut);
        emittersByUserId.compute(userId, (key, emitters) -> {
            if (emitters == null) {
                emitters = ConcurrentHashMap.newKeySet();
            }
            emitters.add(emitter);
            return emitters;
        });
    }
}
