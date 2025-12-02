package com.youtube.api.livestreaming;

import com.youtube.api.config.RestAssuredTest;
import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.testfixtures.builder.UserBuilder;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import com.youtube.live.interaction.livestreaming.domain.ReactionType;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateRequest;
import com.youtube.live.interaction.livestreaming.controller.dto.ReactionCreateRequest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.SubscriptionBuilder.Subscription;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.LiveStreaming;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class LiveStreamingControllerTest extends RestAssuredTest {

    @Test
    @DisplayName("로그인한 사용자가 좋아요를 누른다")
    void toggleLike_Authenticated_ReturnsLikeCountAndReactionStatus() {
        // given
        final Long userId = TestAuthSupport.signUp(
                "test@example.com",
                "테스트유저",
                "password123!"
        ).as(Long.class);

        final User user = UserBuilder.User().withId(userId).build();
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        final String jsessionId = TestAuthSupport.login("test@example.com", "password123!");
        final ReactionCreateRequest request = new ReactionCreateRequest(ReactionType.LIKE);

        // when
        final ExtractableResponse<Response> response = given()
                .cookie("JSESSIONID", jsessionId)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams/{liveStreamingId}/likes", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        // then
        assertThat(response.jsonPath().getInt("likeCount")).isEqualTo(1);
        assertThat(response.jsonPath().getBoolean("liked")).isTrue();
        assertThat(response.jsonPath().getBoolean("disliked")).isFalse();
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 좋아요 누를 수 없다")
    void toggleLike_Unauthenticated_ReturnsError() {
        // given
        final Long userId = TestAuthSupport.signUp(
                "test@example.com",
                "테스트유저",
                "password123!"
        ).as(Long.class);

        final User user = UserBuilder.User().withId(userId).build();
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        final ReactionCreateRequest request = new ReactionCreateRequest(ReactionType.LIKE);

        // when & then
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams/{liveStreamingId}/likes", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("로그인한 사용자가 좋아요 상태를 조회하면 좋아요 개수와 자신의 반응 상태를 받는다")
    void getLikeStatus_Authenticated_ReturnsLikeCountAndUserReactionStatus() {
        // given
        final Long userId1 = TestAuthSupport.signUp(
                "user1@example.com",
                "유저1",
                "password123!"
        ).as(Long.class);

        TestAuthSupport.signUp(
                "user2@example.com",
                "유저2",
                "password123!"
        );

        final User user1 = UserBuilder.User().withId(userId1).build();
        final Channel channel = testSupport.save(Channel().withUser(user1).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        final String jsessionId1 = TestAuthSupport.login("user1@example.com", "password123!");
        final String jsessionId2 = TestAuthSupport.login("user2@example.com", "password123!");
        final ReactionCreateRequest request = new ReactionCreateRequest(ReactionType.LIKE);

        // 두 명의 사용자가 좋아요를 누름
        given()
                .cookie("JSESSIONID", jsessionId1)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams/{liveStreamingId}/likes", liveStreaming.getId());

        given()
                .cookie("JSESSIONID", jsessionId2)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams/{liveStreamingId}/likes", liveStreaming.getId());

        // when - user1이 좋아요 상태를 조회
        final ExtractableResponse<Response> response = given()
                .cookie("JSESSIONID", jsessionId1)
                .when()
                .get("/api/v1/livestreams/{liveStreamingId}/likes", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        // then
        assertThat(response.jsonPath().getInt("likeCount")).isEqualTo(2);
        assertThat(response.jsonPath().getBoolean("isLiked")).isTrue();
        assertThat(response.jsonPath().getBoolean("isDisliked")).isFalse();
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 좋아요 상태를 조회하면 좋아요 개수만 받는다")
    void getLikeStatus_Unauthenticated_ReturnsLikeCountOnly() {
        // given
        final Long userId1 = TestAuthSupport.signUp(
                "user1@example.com",
                "유저1",
                "password123!"
        ).as(Long.class);

        TestAuthSupport.signUp(
                "user2@example.com",
                "유저2",
                "password123!"
        );

        final User user1 = UserBuilder.User().withId(userId1).build();
        final Channel channel = testSupport.save(Channel().withUser(user1).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        final String jsessionId1 = TestAuthSupport.login("user1@example.com", "password123!");
        final String jsessionId2 = TestAuthSupport.login("user2@example.com", "password123!");
        final ReactionCreateRequest request = new ReactionCreateRequest(ReactionType.LIKE);

        // 두 명의 사용자가 좋아요를 누름
        given()
                .cookie("JSESSIONID", jsessionId1)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams/{liveStreamingId}/likes", liveStreaming.getId());

        given()
                .cookie("JSESSIONID", jsessionId2)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams/{liveStreamingId}/likes", liveStreaming.getId());

        // when - 로그인하지 않은 사용자가 좋아요 상태를 조회
        final ExtractableResponse<Response> response = given()
                .when()
                .get("/api/v1/livestreams/{liveStreamingId}/likes", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        // then - 좋아요 개수는 2이고, 반응 상태는 false
        assertThat(response.jsonPath().getInt("likeCount")).isEqualTo(2);
        assertThat(response.jsonPath().getBoolean("isLiked")).isFalse();
        assertThat(response.jsonPath().getBoolean("isDisliked")).isFalse();
    }

    @Test
    @DisplayName("로그인한 사용자가 라이브 스트리밍을 시작한다")
    void startLiveStreaming_Authenticated_ReturnsCreatedLiveStreaming() {
        // given
        TestAuthSupport.signUp(
                "streamer@example.com",
                "스트리머",
                "password123!"
        );

        final String jsessionId = TestAuthSupport.login("streamer@example.com", "password123!");
        final LiveStreamingCreateRequest request = new LiveStreamingCreateRequest(
                "테스트 라이브",
                "테스트 라이브 설명입니다",
                "https://example.com/thumbnail.jpg"
        );

        // when
        final ExtractableResponse<Response> response = given()
                .cookie("JSESSIONID", jsessionId)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract();

        // then
        assertThat(response.jsonPath().getLong("liveStreamingId")).isNotNull();
        assertThat(response.jsonPath().getString("title")).isEqualTo("테스트 라이브");
        assertThat(response.jsonPath().getString("description")).isEqualTo("테스트 라이브 설명입니다");
        assertThat(response.jsonPath().getString("thumbnailUrl")).isEqualTo("https://example.com/thumbnail.jpg");
        assertThat(response.jsonPath().getString("status")).isEqualTo(LiveStreamingStatus.LIVE.name());
        assertThat(response.jsonPath().getLong("channelId")).isNotNull();
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 라이브 스트리밍을 시작할 수 없다")
    void startLiveStreaming_Unauthenticated_ReturnsError() {
        // given
        final LiveStreamingCreateRequest request = new LiveStreamingCreateRequest(
                "테스트 라이브",
                "테스트 라이브 설명입니다",
                "https://example.com/thumbnail.jpg"
        );

        // when & then
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/livestreams")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("라이브 스트리밍 메타데이터를 조회하면 채널 정보와 라이브 정보 및 구독자 수를 받는다")
    void getMetadata_ReturnsLiveStreamingMetadata() {
        // given
        final Long streamerId = TestAuthSupport.signUp(
                "streamer@example.com",
                "스트리머",
                "password123!"
        ).as(Long.class);

        final Long subscriber1Id = TestAuthSupport.signUp(
                "subscriber1@example.com",
                "구독자1",
                "password123!"
        ).as(Long.class);

        final Long subscriber2Id = TestAuthSupport.signUp(
                "subscriber2@example.com",
                "구독자2",
                "password123!"
        ).as(Long.class);

        final User streamer = UserBuilder.User().withId(streamerId).build();
        final User subscriber1 = UserBuilder.User().withId(subscriber1Id).build();
        final User subscriber2 = UserBuilder.User().withId(subscriber2Id).build();

        final Channel channel = testSupport.save(
                Channel()
                        .withUser(streamer)
                        .withChannelName("테스트 채널")
                        .withProfileImageUrl("https://example.com/profile.jpg")
                        .build()
        );

        final LiveStreaming liveStreaming = testSupport.save(
                LiveStreaming()
                        .withChannel(channel)
                        .withTitle("테스트 라이브")
                        .withDescription("테스트 라이브 설명입니다")
                        .build()
        );

        testSupport.save(Subscription().withSubscriber(subscriber1).withChannel(channel).build());
        testSupport.save(Subscription().withSubscriber(subscriber2).withChannel(channel).build());

        // when
        final ExtractableResponse<Response> response = given()
                .when()
                .get("/api/v1/livestreams/{liveStreamingId}/metadata", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        // then
        assertThat(response.jsonPath().getLong("channelId")).isEqualTo(channel.getId());
        assertThat(response.jsonPath().getString("channelName")).isEqualTo("테스트 채널");
        assertThat(response.jsonPath().getString("channelProfileImageUrl")).isEqualTo("https://example.com/profile.jpg");
        assertThat(response.jsonPath().getString("liveStreamingTitle")).isEqualTo("테스트 라이브");
        assertThat(response.jsonPath().getString("liveStreamingDescription")).isEqualTo("테스트 라이브 설명입니다");
        assertThat(response.jsonPath().getString("liveStreamingStartedAt")).isNotNull();
        assertThat(response.jsonPath().getLong("subscriberCount")).isEqualTo(2);
    }
}
