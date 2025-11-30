package com.youtube.api.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtube.api.config.RestAssuredTest;
import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.api.testfixtures.support.TestSseSession;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.channel.domain.QChannel;
import com.youtube.core.user.domain.QUser;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateRequest;
import com.youtube.notification.event.NotificationCreatedEvent;
import com.youtube.notification.service.dto.NotificationReadResponse;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static com.youtube.core.testfixtures.builder.SubscriptionBuilder.Subscription;
import static com.youtube.notification.testfixtures.builder.NotificationBuilder.Notification;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class NotificationControllerTest extends RestAssuredTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("로그인한 사용자가 알림을 조회하면 성공적으로 알림 목록을 반환한다")
    void getNotifications() {
        // given
        final Long userId = TestAuthSupport.signUp("user@example.com", "testuser", "password123").as(Long.class);
        final String sessionCookie = TestAuthSupport.login("user@example.com", "password123");

        final User receiver = findUserById(userId);
        testSupport.saveAll(
                Notification().withReceiver(receiver).build(),
                Notification().withReceiver(receiver).build(),
                Notification().withReceiver(receiver).build()
        );

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .get("/api/v1/notifications")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        final NotificationReadResponse notificationResponse = response.as(NotificationReadResponse.class);
        assertThat(notificationResponse.notifications()).hasSize(3);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 알림을 조회하면 오류가 발생한다")
    void getNotificationsWithoutLogin() {
        // when
        final ExtractableResponse<Response> response = given().log().all()
                .when()
                .get("/api/v1/notifications")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("구독자가 알림 연결을 열어두면 스트리머의 라이브 시작 시 서버로부터 SSE를 통해 알림을 받는다")
    void receiveLiveNotificationThroughSseWhenStreamerStartsLive() throws Exception {
        // given
        final Long streamerId = TestAuthSupport.signUp("streamer@example.com", "스트리머", "password123!").as(Long.class);
        final Long subscriberId = TestAuthSupport.signUp("subscriber@example.com", "구독자", "password123!").as(Long.class);
        final String subscriberJsessionId = TestAuthSupport.login("subscriber@example.com", "password123!");

        final User subscriber = findUserById(subscriberId);
        final Channel streamerChannel = findChannelByUserId(streamerId);
        testSupport.save(Subscription().withSubscriber(subscriber).withChannel(streamerChannel).build());

        final TestSseSession<NotificationCreatedEvent> sseSession = TestSseSession.connect(
                port,
                "/api/v1/notifications/stream",
                subscriberJsessionId,
                NotificationCreatedEvent.class,
                objectMapper,
                "notification"
        );

        // when
        final String streamerSession = TestAuthSupport.login("streamer@example.com", "password123!");
        final LiveStreamingCreateRequest request = new LiveStreamingCreateRequest(
                "테스트 라이브",
                "테스트 라이브 설명",
                "https://example.com/thumbnail.jpg"
        );

        given()
                .cookie("JSESSIONID", streamerSession)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        // then
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final NotificationCreatedEvent event = sseSession.getLatestEvent();
                    assertThat(event).isNotNull();
                    assertThat(event.receiverId()).isEqualTo(subscriberId);
                    assertThat(event.title()).contains("테스트 라이브");
                });

        sseSession.disconnect();
    }

    @Test
    @DisplayName("로그인한 사용자가 읽지 않은 알림 개수를 조회한다")
    void getUnreadCount() {
        // given
        final Long userId = TestAuthSupport.signUp("user@example.com", "testuser", "password123").as(Long.class);
        final String sessionCookie = TestAuthSupport.login("user@example.com", "password123");

        final User receiver = findUserById(userId);
        testSupport.saveAll(
                Notification().withReceiver(receiver).withRead(false).build(),
                Notification().withReceiver(receiver).withRead(false).build(),
                Notification().withReceiver(receiver).withRead(false).build(),
                Notification().withReceiver(receiver).withRead(true).build(),
                Notification().withReceiver(receiver).withRead(true).build()
        );

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .get("/api/v1/notifications/unread-count")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.as(Long.class)).isEqualTo(3L);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 읽지 않은 알림 개수를 조회하면 오류가 발생한다")
    void getUnreadCountWithoutLogin() {
        // when
        final ExtractableResponse<Response> response = given().log().all()
                .when()
                .get("/api/v1/notifications/unread-count")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("로그인한 사용자가 모든 알림을 읽음 처리하면 모든 알림이 읽음 상태로 변경되고 SSE로 읽지 않은 개수가 전송된다")
    void markAllAsRead() {
        // given
        final Long userId = TestAuthSupport.signUp("user@example.com", "testuser", "password123").as(Long.class);
        final String sessionCookie = TestAuthSupport.login("user@example.com", "password123");

        final User receiver = findUserById(userId);
        testSupport.saveAll(
                Notification().withReceiver(receiver).withRead(false).build(),
                Notification().withReceiver(receiver).withRead(false).build(),
                Notification().withReceiver(receiver).withRead(false).build()
        );

        final TestSseSession<Long> sseSession = TestSseSession.connect(
                port,
                "/api/v1/notifications/stream",
                sessionCookie,
                Long.class,
                objectMapper,
                "unread-count"
        );

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .post("/api/v1/notifications/read-all")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.as(Long.class)).isEqualTo(3L);

        // DB에서 실제로 읽음 처리되었는지 확인
        final ExtractableResponse<Response> unreadCountResponse = given()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .get("/api/v1/notifications/unread-count")
                .then()
                .extract();

        assertThat(unreadCountResponse.as(Long.class)).isEqualTo(0L);

        // SSE를 통해 읽지 않은 개수가 0으로 전송되었는지 확인
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    final Long unreadCount = sseSession.getLatestEvent();
                    assertThat(unreadCount).isNotNull();
                    assertThat(unreadCount).isEqualTo(0L);
                });

        sseSession.disconnect();
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 모든 알림을 읽음 처리하려고 하면 오류가 발생한다")
    void markAllAsReadWithoutLogin() {
        // when
        final ExtractableResponse<Response> response = given().log().all()
                .when()
                .post("/api/v1/notifications/read-all")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private User findUserById(final Long userId) {
        return testSupport.jpaQueryFactory
                .selectFrom(QUser.user)
                .where(QUser.user.id.eq(userId))
                .fetchOne();
    }

    private Channel findChannelByUserId(final Long userId) {
        return testSupport.jpaQueryFactory
                .selectFrom(QChannel.channel)
                .where(QChannel.channel.user.id.eq(userId))
                .fetchOne();
    }
}
