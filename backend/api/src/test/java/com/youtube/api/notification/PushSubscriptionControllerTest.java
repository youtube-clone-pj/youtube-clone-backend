package com.youtube.api.notification;

import com.youtube.api.config.RestAssuredTest;
import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.user.domain.QUser;
import com.youtube.core.user.domain.User;
import com.youtube.notification.service.dto.PushSubscribeRequest;
import com.youtube.notification.service.dto.PushUnsubscribeRequest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.youtube.notification.testfixtures.builder.PushSubscriptionBuilder.PushSubscription;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class PushSubscriptionControllerTest extends RestAssuredTest {

    @Test
    @DisplayName("로그인한 사용자가 푸시 알림을 구독하면 성공한다")
    void subscribe_LoggedInUser_Success() {
        // given
        final Long userId = TestAuthSupport.signUp("user@example.com", "testuser", "password123").as(Long.class);
        final String sessionCookie = TestAuthSupport.login("user@example.com", "password123");

        final PushSubscribeRequest request = new PushSubscribeRequest(
                "https://fcm.googleapis.com/fcm/send/test-endpoint",
                new PushSubscribeRequest.Keys("p256dh-key", "auth-key"),
                "Mozilla/5.0"
        );

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/push/subscribe")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 푸시 알림을 구독하려고 하면 오류가 발생한다")
    void subscribe_NotLoggedIn_Error() {
        // given
        final PushSubscribeRequest request = new PushSubscribeRequest(
                "https://fcm.googleapis.com/fcm/send/test-endpoint",
                new PushSubscribeRequest.Keys("p256dh-key", "auth-key"),
                "Mozilla/5.0"
        );

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/push/subscribe")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("로그인한 사용자가 푸시 알림 구독을 해제하면 성공한다")
    void unsubscribe_LoggedInUser_Success() {
        // given
        final Long userId = TestAuthSupport.signUp("user@example.com", "testuser", "password123").as(Long.class);
        final String sessionCookie = TestAuthSupport.login("user@example.com", "password123");

        final User user = findUserById(userId);
        final String endpoint = "https://fcm.googleapis.com/fcm/send/unsubscribe-endpoint";
        testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withEndpoint(endpoint)
                        .build()
        );

        final PushUnsubscribeRequest request = new PushUnsubscribeRequest(endpoint);

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .delete("/api/v1/push/unsubscribe")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 푸시 알림 구독을 해제하려고 하면 오류가 발생한다")
    void unsubscribe_NotLoggedIn_Error() {
        // given
        final PushUnsubscribeRequest request = new PushUnsubscribeRequest(
                "https://fcm.googleapis.com/fcm/send/test-endpoint"
        );

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .delete("/api/v1/push/unsubscribe")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("로그인한 사용자가 모든 푸시 알림 구독을 비활성화하면 성공한다")
    void deactivateAllSubscriptions_LoggedInUser_Success() {
        // given
        final Long userId = TestAuthSupport.signUp("user@example.com", "testuser", "password123").as(Long.class);
        final String sessionCookie = TestAuthSupport.login("user@example.com", "password123");

        final User user = findUserById(userId);
        testSupport.saveAll(
                PushSubscription().withUser(user).withEndpoint("endpoint-1").withActive(true).build(),
                PushSubscription().withUser(user).withEndpoint("endpoint-2").withActive(true).build(),
                PushSubscription().withUser(user).withEndpoint("endpoint-3").withActive(true).build()
        );

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .post("/api/v1/push/deactivate")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 모든 푸시 알림 구독을 비활성화하려고 하면 오류가 발생한다")
    void deactivateAllSubscriptions_NotLoggedIn_Error() {
        // when
        final ExtractableResponse<Response> response = given().log().all()
                .when()
                .post("/api/v1/push/deactivate")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("로그인한 사용자가 특정 푸시 알림 구독을 재활성화하면 성공한다")
    void reactivateSubscription_LoggedInUser_Success() {
        // given
        final Long userId = TestAuthSupport.signUp("user@example.com", "testuser", "password123").as(Long.class);
        final String sessionCookie = TestAuthSupport.login("user@example.com", "password123");

        final User user = findUserById(userId);
        final String endpoint = "https://fcm.googleapis.com/fcm/send/reactivate-endpoint";
        testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .withEndpoint(endpoint)
                        .withActive(false)
                        .build()
        );

        final PushUnsubscribeRequest request = new PushUnsubscribeRequest(endpoint);

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/push/reactivate")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 푸시 알림 구독을 재활성화하려고 하면 오류가 발생한다")
    void reactivateSubscription_NotLoggedIn_Error() {
        // given
        final PushUnsubscribeRequest request = new PushUnsubscribeRequest(
                "https://fcm.googleapis.com/fcm/send/test-endpoint"
        );

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/push/reactivate")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private User findUserById(final Long userId) {
        return testSupport.jpaQueryFactory
                .selectFrom(QUser.user)
                .where(QUser.user.id.eq(userId))
                .fetchOne();
    }
}
