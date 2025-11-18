package com.youtube.api.subscription;

import com.youtube.api.config.RestAssuredTest;
import com.youtube.api.subscription.dto.SubscribeResponse;
import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionControllerTest extends RestAssuredTest {

    @Test
    @DisplayName("로그인한 사용자가 채널을 구독하면 구독이 성공적으로 생성된다")
    void subscribe() {
        // given
        TestAuthSupport.signUp("subscriber@example.com", "subscriber", "password123");
        final String sessionCookie = TestAuthSupport.login("subscriber@example.com", "password123");

        final User channelOwner = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(channelOwner).build());

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .post("/api/v1/channels/" + channel.getId() + "/subscriptions")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        final SubscribeResponse subscribeResponse = response.as(SubscribeResponse.class);
        assertThat(subscribeResponse.isSubscribed()).isTrue();
    }

    @Test
    @DisplayName("로그인한 사용자가 구독한 채널을 구독 취소하면 성공적으로 구독이 취소된다")
    void unsubscribe() {
        // given

        TestAuthSupport.signUp("subscriber@example.com", "subscriber", "password123");
        final String sessionCookie = TestAuthSupport.login("subscriber@example.com", "password123");

        final User channelOwner = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(channelOwner).build());

        // 먼저 구독
        given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .post("/api/v1/channels/" + channel.getId() + "/subscriptions")
                .then()
                .log().all()
                .extract();

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .delete("/api/v1/channels/" + channel.getId() + "/subscriptions")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        final SubscribeResponse subscribeResponse = response.as(SubscribeResponse.class);
        assertThat(subscribeResponse.isSubscribed()).isFalse();
    }

    @Test
    @DisplayName("로그인한 사용자가 구독한 채널의 상태를 조회한다")
    void getSubscriptionStatusWhenSubscribed() {
        // given

        TestAuthSupport.signUp("subscriber@example.com", "subscriber", "password123");
        final String sessionCookie = TestAuthSupport.login("subscriber@example.com", "password123");

        final User channelOwner = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(channelOwner).build());

        // 먼저 구독
        given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .post("/api/v1/channels/" + channel.getId() + "/subscriptions")
                .then()
                .log().all()
                .extract();

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .get("/api/v1/channels/" + channel.getId() + "/subscriptions/status")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.as(Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 구독 상태를 조회하면 false를 반환한다")
    void getSubscriptionStatusWhenNotLoggedIn() {
        // given
        final User channelOwner = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(channelOwner).build());

        // when
        final ExtractableResponse<Response> response = given().log().all()
                .when()
                .get("/api/v1/channels/" + channel.getId() + "/subscriptions/status")
                .then()
                .log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.as(Boolean.class)).isFalse();
    }
}
