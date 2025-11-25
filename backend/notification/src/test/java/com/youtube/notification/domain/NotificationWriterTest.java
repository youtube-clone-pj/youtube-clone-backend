package com.youtube.notification.domain;

import com.youtube.core.user.domain.User;
import com.youtube.notification.config.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.notification.testfixtures.builder.NotificationBuilder.Notification;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationWriterTest extends IntegrationTest {

    @Autowired
    private NotificationWriter sut;

    @Test
    @DisplayName("읽지 않은 알림들을 모두 읽음 처리한다")
    void markAllAsRead_UnreadNotifications_MarksAllAsReadAndReturnsCount() {
        // given
        final User receiver = testSupport.save(User().build());

        testSupport.saveAll(
                Notification().withReceiver(receiver).withRead(false).build(),
                Notification().withReceiver(receiver).withRead(false).build(),
                Notification().withReceiver(receiver).withRead(false).build()
        );

        // when
        final long markedCount = sut.markAllAsRead(receiver.getId());

        // then
        assertThat(markedCount).isEqualTo(3);
    }


    @Test
    @DisplayName("읽지 않은 알림이 없으면 0을 반환한다")
    void markAllAsRead_NoUnreadNotifications_ReturnsZero() {
        // given
        final User receiver = testSupport.save(User().build());

        testSupport.saveAll(
                Notification().withReceiver(receiver).withRead(true).build(),
                Notification().withReceiver(receiver).withRead(true).build()
        );

        // when
        final long markedCount = sut.markAllAsRead(receiver.getId());

        // then
        assertThat(markedCount).isZero();
    }

}
