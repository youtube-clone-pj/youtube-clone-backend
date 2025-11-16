package com.youtube.api.livestreaming;

import com.youtube.api.config.RestAssuredTest;
import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.testfixtures.builder.UserBuilder;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.ReactionType;
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
}
