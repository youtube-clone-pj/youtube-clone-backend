package com.youtube.api.notification;

import com.youtube.api.config.RestAssuredTest;
import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.user.domain.QUser;
import com.youtube.core.user.domain.User;
import com.youtube.notification.service.dto.NotificationReadResponse;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.youtube.notification.testfixtures.builder.NotificationBuilder.Notification;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationControllerTest extends RestAssuredTest {

    @Test
    @DisplayName("로그인한 사용자가 알림을 조회하면 성공적으로 알림 목록을 반환한다")
    void getNotifications() {
        // given
        final Long userId = TestAuthSupport.signUp("user@example.com", "testuser", "password123").as(Long.class);
        final String sessionCookie = TestAuthSupport.login("user@example.com", "password123");

        final User receiver = findUserById(userId);
        testSupport.save(Notification().withReceiver(receiver).build());
        testSupport.save(Notification().withReceiver(receiver).build());
        testSupport.save(Notification().withReceiver(receiver).build());

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
        assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private User findUserById(final Long userId) {
        return testSupport.jpaQueryFactory
                .selectFrom(QUser.user)
                .where(QUser.user.id.eq(userId))
                .fetchOne();
    }
}
