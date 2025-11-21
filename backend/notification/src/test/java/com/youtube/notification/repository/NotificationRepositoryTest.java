package com.youtube.notification.repository;

import com.youtube.core.user.domain.User;
import com.youtube.notification.config.IntegrationTest;
import com.youtube.notification.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.notification.testfixtures.builder.NotificationBuilder.Notification;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryTest extends IntegrationTest {

    @Autowired
    private NotificationRepository sut;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("커서가 null일 때 최신 알림부터 조회된다")
    void findByReceiverIdWithCursor_NullCursor_ReturnsLatestNotifications() {
        // given
        final User receiver = testSupport.save(User().build());

        // 3개의 알림 생성
        List<Notification> notifications = testSupport.saveAll(
                Notification().withReceiver(receiver).build(),
                Notification().withReceiver(receiver).build(),
                Notification().withReceiver(receiver).build()
        );

        // when
        final List<Notification> result = sut.findByReceiverIdWithCursor(
                receiver.getId(),
                null,
                Instant.now().minus(35, ChronoUnit.DAYS),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).hasSize(3);
        // ID가 큰 순서대로 조회되는지 확인 (최신순)
        assertThat(result.get(0).getId()).isEqualTo(notifications.getLast().getId());
        assertThat(result.get(2).getId()).isEqualTo(notifications.getFirst().getId());
    }

    @Test
    @DisplayName("커서가 있을 때 커서보다 작은 ID의 알림들만 조회된다")
    void findByReceiverIdWithCursor_WithCursor_ReturnsNotificationsBeforeCursor() {
        // given
        final User receiver = testSupport.save(User().build());

        // 5개의 알림 생성
        final Notification notification1 = testSupport.save(Notification().withReceiver(receiver).build());
        final Notification notification2 = testSupport.save(Notification().withReceiver(receiver).build());
        final Notification notification3 = testSupport.save(Notification().withReceiver(receiver).build());
        final Notification notification4 = testSupport.save(Notification().withReceiver(receiver).build());

        // when
        final List<Notification> result = sut.findByReceiverIdWithCursor(
                receiver.getId(),
                notification3.getId(),
                Instant.now().minus(35, ChronoUnit.DAYS),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).hasSize(2);
        // cursor보다 작은 ID만 조회되는지 확인
        assertThat(result.get(0).getId()).isEqualTo(notification2.getId());
        assertThat(result.get(1).getId()).isEqualTo(notification1.getId());
    }

    @Test
    @DisplayName("5주 이전의 알림은 조회되지 않는다")
    void findByReceiverIdWithCursor_OldNotifications_NotReturned() {
        // given
        final User receiver = testSupport.save(User().build());

        // 4주 전 알림 (조회됨)
        final Notification recentNotification = testSupport.save(
                Notification()
                        .withReceiver(receiver)
                        .build()
        );
        updateCreatedDate(recentNotification.getId(), Instant.now().minus(28, ChronoUnit.DAYS));

        // 6주 전 알림 (조회 안됨)
        final Notification oldNotification = testSupport.save(
                Notification()
                        .withReceiver(receiver)
                        .build()
        );
        updateCreatedDate(oldNotification.getId(), Instant.now().minus(42, ChronoUnit.DAYS));

        // when
        final List<Notification> result = sut.findByReceiverIdWithCursor(
                receiver.getId(),
                null,
                Instant.now().minus(35, ChronoUnit.DAYS),
                PageRequest.of(0, 10)
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(recentNotification.getId());
    }

    private void updateCreatedDate(final Long notificationId, final Instant createdDate) {
        jdbcTemplate.update(
                "UPDATE notification SET created_date = ? WHERE id = ?",
                java.sql.Timestamp.from(createdDate),
                notificationId
        );
    }
}