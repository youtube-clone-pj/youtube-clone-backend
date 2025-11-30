package com.youtube.notification.service;

import com.youtube.core.user.domain.User;
import com.youtube.notification.config.IntegrationTest;
import com.youtube.notification.domain.PushSubscription;
import com.youtube.notification.domain.QPushSubscription;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.notification.testfixtures.builder.PushSubscriptionBuilder.PushSubscription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebPushServiceTest extends IntegrationTest {

    @Autowired
    private WebPushService sut;

    @MockitoBean
    private WebPushSender webPushSender;

    @Test
    @DisplayName("활성 상태의 구독에만 푸시 알림이 전송된다")
    void sendNotification_OnlyActiveSubscriptions_SendPush() {
        // given
        final User user = testSupport.save(User().build());
        final PushSubscription activeSubscription = testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withEndpoint("active-endpoint")
                        .withActive(true)
                        .build()
        );
        testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withEndpoint("inactive-endpoint")
                        .withActive(false)
                        .build()
        );

        final Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(201);
        when(webPushSender.sendAsync(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // when
        sut.sendNotification(user.getId(), "{\"title\":\"Test\"}");

        // then
        verify(webPushSender, times(1))
                .sendAsync(
                        eq(activeSubscription.getEndpoint()),
                        eq(activeSubscription.getP256dh()),
                        eq(activeSubscription.getAuth()),
                        eq("{\"title\":\"Test\"}")
                );
    }

    @Test
    @DisplayName("410 Gone 응답 시 구독이 삭제된다")
    void sendNotification_410Gone_DeleteSubscription() {
        // given
        final User user = testSupport.save(User().build());
        final PushSubscription subscription = testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withActive(true)
                        .build()
        );

        final Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(410);
        when(webPushSender.sendAsync(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // when
        sut.sendNotification(user.getId(), "{\"title\":\"Test\"}");

        // then
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final PushSubscription foundSubscription = testSupport.jpaQueryFactory
                            .selectFrom(QPushSubscription.pushSubscription)
                            .where(QPushSubscription.pushSubscription.id.eq(subscription.getId()))
                            .fetchOne();

                    assertThat(foundSubscription).isNull();
                });
    }

    @Test
    @DisplayName("404 Not Found 응답 시 구독이 삭제된다")
    void sendNotification_404NotFound_DeleteSubscription() {
        // given
        final User user = testSupport.save(User().build());
        final PushSubscription subscription = testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withActive(true)
                        .build()
        );

        final Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(404);
        when(webPushSender.sendAsync(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // when
        sut.sendNotification(user.getId(), "{\"title\":\"Test\"}");

        // then
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final PushSubscription foundSubscription = testSupport.jpaQueryFactory
                            .selectFrom(QPushSubscription.pushSubscription)
                            .where(QPushSubscription.pushSubscription.id.eq(subscription.getId()))
                            .fetchOne();

                    assertThat(foundSubscription).isNull();
                });
    }

    @Test
    @DisplayName("410, 404 외의 오류 발생 시 구독이 삭제되지 않는다")
    void sendNotification_OtherError_SubscriptionNotDeleted() {
        // given
        final User user = testSupport.save(User().build());
        final PushSubscription subscription = testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withActive(true)
                        .build()
        );

        when(webPushSender.sendAsync(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Network error")
                ));

        // when
        sut.sendNotification(user.getId(), "{\"title\":\"Test\"}");

        // then
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final PushSubscription existingSubscription = testSupport.jpaQueryFactory
                            .selectFrom(QPushSubscription.pushSubscription)
                            .where(QPushSubscription.pushSubscription.id.eq(subscription.getId()))
                            .fetchOne();

                    assertThat(existingSubscription).isNotNull();
                });
    }

    @Test
    @DisplayName("사용자가 여러 구독을 가지고 있으면 모든 활성 구독에 푸시를 전송한다")
    void sendNotification_MultipleActiveSubscriptions_SendToAll() {
        // given
        final User user = testSupport.save(User().build());
        final PushSubscription subscription1 = testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withEndpoint("endpoint-1")
                        .withActive(true)
                        .build()
        );
        final PushSubscription subscription2 = testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withEndpoint("endpoint-2")
                        .withActive(true)
                        .build()
        );
        final PushSubscription subscription3 = testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withEndpoint("endpoint-3")
                        .withActive(true)
                        .build()
        );

        final Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(201);
        when(webPushSender.sendAsync(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // when
        sut.sendNotification(user.getId(), "{\"title\":\"Test\"}");

        // then
        verify(webPushSender, times(3))
                .sendAsync(anyString(), anyString(), anyString(), eq("{\"title\":\"Test\"}"));
    }
}
