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
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("라이브 스트리밍의 좋아요 개수를 조회한다")
    void getLikeCount_ReturnsLikeCount() {
        // given
        final Long userId1 = TestAuthSupport.signUp(
                "user1@example.com",
                "유저1",
                "password123!"
        ).as(Long.class);

        final Long userId2 = TestAuthSupport.signUp(
                "user2@example.com",
                "유저2",
                "password123!"
        ).as(Long.class);

        final User user1 = UserBuilder.User().withId(userId1).build();
        final User user2 = UserBuilder.User().withId(userId2).build();
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

        // when
        final ExtractableResponse<Response> response = given()
                .when()
                .get("/api/v1/livestreams/{liveStreamingId}/likes/count", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        // then
        assertThat(response.as(Integer.class)).isEqualTo(2);
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
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
