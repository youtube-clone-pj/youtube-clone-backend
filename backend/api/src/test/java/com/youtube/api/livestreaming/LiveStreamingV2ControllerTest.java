package com.youtube.api.livestreaming;

import com.youtube.api.config.RestAssuredTest;
import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.testfixtures.builder.UserBuilder;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingChat;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateRequest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.SubscriptionBuilder.Subscription;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.LiveStreaming;
import static com.youtube.live.interaction.builder.LiveStreamingChatBuilder.LiveStreamingChat;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class LiveStreamingV2ControllerTest extends RestAssuredTest {

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
                .post("/api/v2/livestreams")
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
                .post("/api/v2/livestreams")
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
                .get("/api/v2/livestreams/{liveStreamingId}/metadata", liveStreaming.getId())
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

    @Test
    @DisplayName("라이브 통계를 조회하면 시청자 수와 좋아요 수를 받는다")
    void getLiveStats_ReturnsViewerCountAndLikeCount() {
        // given
        final Long streamerId = TestAuthSupport.signUp(
                "streamer@example.com",
                "스트리머",
                "password123!"
        ).as(Long.class);

        final User streamer = UserBuilder.User().withId(streamerId).build();
        final Channel channel = testSupport.save(Channel().withUser(streamer).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        // when
        final ExtractableResponse<Response> response = given()
                .when()
                .get("/api/v2/livestreams/{liveStreamingId}/live-stats", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        // then
        assertThat(response.jsonPath().getInt("viewerCount")).isGreaterThanOrEqualTo(0);
        assertThat(response.jsonPath().getInt("likeCount")).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("lastChatId 없이 채팅을 조회하면 최근 채팅 목록을 받는다")
    void getChats_WithoutLastChatId_ReturnsRecentChats() {
        // given
        final Long streamerId = TestAuthSupport.signUp(
                "streamer@example.com",
                "스트리머",
                "password123!"
        ).as(Long.class);

        final Long user1Id = TestAuthSupport.signUp(
                "user1@example.com",
                "유저1",
                "password123!"
        ).as(Long.class);

        final Long user2Id = TestAuthSupport.signUp(
                "user2@example.com",
                "유저2",
                "password123!"
        ).as(Long.class);

        final User streamer = UserBuilder.User().withId(streamerId).build();
        final User user1 = UserBuilder.User().withId(user1Id).build();
        final User user2 = UserBuilder.User().withId(user2Id).build();

        final Channel channel = testSupport.save(Channel().withUser(streamer).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        testSupport.save(
                LiveStreamingChat()
                        .withLiveStreaming(liveStreaming)
                        .withUser(user1)
                        .withMessage("첫 번째 채팅")
                        .build()
        );

        testSupport.save(
                LiveStreamingChat()
                        .withLiveStreaming(liveStreaming)
                        .withUser(user2)
                        .withMessage("두 번째 채팅")
                        .build()
        );

        // when
        final ExtractableResponse<Response> response = given()
                .when()
                .get("/api/v2/livestreams/{liveStreamingId}/chats", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        // then
        assertThat(response.jsonPath().getList("chats")).hasSize(2);
        assertThat(response.jsonPath().getLong("lastChatId")).isNotNull();
    }

    @Test
    @DisplayName("lastChatId와 함께 채팅을 조회하면 이후의 새로운 채팅 목록을 받는다")
    void getChats_WithLastChatId_ReturnsNewChats() {
        // given
        final Long streamerId = TestAuthSupport.signUp(
                "streamer@example.com",
                "스트리머",
                "password123!"
        ).as(Long.class);

        final Long user1Id = TestAuthSupport.signUp(
                "user1@example.com",
                "유저1",
                "password123!"
        ).as(Long.class);

        final Long user2Id = TestAuthSupport.signUp(
                "user2@example.com",
                "유저2",
                "password123!"
        ).as(Long.class);

        final User streamer = UserBuilder.User().withId(streamerId).build();
        final User user1 = UserBuilder.User().withId(user1Id).build();
        final User user2 = UserBuilder.User().withId(user2Id).build();

        final Channel channel = testSupport.save(Channel().withUser(streamer).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        final LiveStreamingChat chat1 = testSupport.save(
                LiveStreamingChat()
                        .withLiveStreaming(liveStreaming)
                        .withUser(user1)
                        .withMessage("첫 번째 채팅")
                        .build()
        );

        testSupport.save(
                LiveStreamingChat()
                        .withLiveStreaming(liveStreaming)
                        .withUser(user2)
                        .withMessage("두 번째 채팅")
                        .build()
        );

        // when
        final ExtractableResponse<Response> response = given()
                .queryParam("lastChatId", chat1.getId())
                .when()
                .get("/api/v2/livestreams/{liveStreamingId}/chats", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        // then
        assertThat(response.jsonPath().getList("chats")).hasSize(1);
        assertThat(response.jsonPath().getString("chats[0].message")).isEqualTo("두 번째 채팅");
        assertThat(response.jsonPath().getLong("lastChatId")).isNotNull();
    }

    @Test
    @DisplayName("lastChatId가 0이면 400 에러를 반환한다")
    void getChats_WithZeroLastChatId_Returns400Error() {
        // given
        final Long anyLiveStreamingId = 1L;

        // when
        final ExtractableResponse<Response> response = given()
                .queryParam("lastChatId", 0)
                .when()
                .get("/api/v2/livestreams/{liveStreamingId}/chats", anyLiveStreamingId)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .extract();

        // then
        assertThat(response.jsonPath().getString("code")).isEqualTo("LIVE_004");
        assertThat(response.jsonPath().getString("message")).isEqualTo("lastChatId는 양수여야 합니다");
    }

    @Test
    @DisplayName("로그인한 사용자가 채팅을 전송하면 성공적으로 생성되고 채팅 정보를 받는다")
    void sendChat_WithAuthenticatedUser_ReturnsCreatedChatInfo() {
        // given
        final Long streamerId = TestAuthSupport.signUp(
                "streamer@example.com",
                "스트리머",
                "password123!"
        ).as(Long.class);

        final Long chatterId = TestAuthSupport.signUp(
                "chatter@example.com",
                "채팅유저",
                "password123!"
        ).as(Long.class);

        final String sessionCookie = TestAuthSupport.login("chatter@example.com", "password123!");

        final User streamer = UserBuilder.User().withId(streamerId).build();
        final Channel channel = testSupport.save(Channel().withUser(streamer).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        final Map<String, String> chatRequest = new HashMap<>();
        chatRequest.put("message", "안녕하세요! 첫 번째 채팅입니다");
        chatRequest.put("chatMessageType", "CHAT");

        // when
        final ExtractableResponse<Response> response = given()
                .cookie("JSESSIONID", sessionCookie)
                .contentType(ContentType.JSON)
                .body(chatRequest)
                .when()
                .post("/api/v2/livestreams/{liveStreamingId}/chats", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract();

        // then
        assertThat(response.jsonPath().getString("username")).isEqualTo("채팅유저");
        assertThat(response.jsonPath().getString("message")).isEqualTo("안녕하세요! 첫 번째 채팅입니다");
        assertThat(response.jsonPath().getString("chatMessageType")).isEqualTo("CHAT");
        assertThat(response.jsonPath().getString("userProfileImageUrl")).isNotNull();
        assertThat(response.jsonPath().getString("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 채팅을 전송하면 401 에러를 받는다")
    void sendChat_WithoutAuthentication_Returns401Error() {
        // given
        final Long streamerId = TestAuthSupport.signUp(
                "streamer@example.com",
                "스트리머",
                "password123!"
        ).as(Long.class);

        final User streamer = UserBuilder.User().withId(streamerId).build();
        final Channel channel = testSupport.save(Channel().withUser(streamer).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        final Map<String, String> chatRequest = new HashMap<>();
        chatRequest.put("message", "인증 없이 채팅 전송");
        chatRequest.put("chatMessageType", "CHAT");

        // when
        final ExtractableResponse<Response> response = given()
                .contentType(ContentType.JSON)
                .body(chatRequest)
                .when()
                .post("/api/v2/livestreams/{liveStreamingId}/chats", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .extract();

        // then
        assertThat(response.jsonPath().getString("code")).isEqualTo("AUTH_001");
        assertThat(response.jsonPath().getString("message")).isEqualTo("로그인이 필요합니다");
    }

    @Test
    @DisplayName("스트리머가 라이브 스트리밍을 종료한다")
    void endLiveStreaming_ByOwner_ReturnsOk() {
        // given
        final Long streamerId = TestAuthSupport.signUp(
                "streamer@example.com",
                "스트리머",
                "password123!"
        ).as(Long.class);

        final String sessionCookie = TestAuthSupport.login("streamer@example.com", "password123!");

        final User streamer = UserBuilder.User().withId(streamerId).build();
        final Channel channel = testSupport.save(Channel().withUser(streamer).build());
        final LiveStreaming liveStreaming = testSupport.save(
                LiveStreaming()
                        .withChannel(channel)
                        .withStatus(LiveStreamingStatus.LIVE)
                        .build()
        );

        // when
        given()
                .cookie("JSESSIONID", sessionCookie)
                .when()
                .post("/api/v2/livestreams/{liveStreamingId}/end", liveStreaming.getId())
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 라이브 스트리밍을 종료할 수 없다")
    void endLiveStreaming_Unauthenticated_Returns401Error() {
        // given
        final Long anyLiveStreamingId = 1L;

        // when
        final ExtractableResponse<Response> response = given()
                .when()
                .post("/api/v2/livestreams/{liveStreamingId}/end", anyLiveStreamingId)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .extract();

        // then
        assertThat(response.jsonPath().getString("code")).isEqualTo("AUTH_001");
        assertThat(response.jsonPath().getString("message")).isEqualTo("로그인이 필요합니다");
    }
}
