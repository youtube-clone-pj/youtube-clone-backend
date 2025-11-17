package com.youtube.live.interaction.websocket.event;

import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.testfixtures.builder.ChannelBuilder;
import com.youtube.core.testfixtures.builder.UserBuilder;
import com.youtube.live.interaction.builder.LiveStreamingBuilder;
import com.youtube.live.interaction.config.WebSocketStompTest;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.support.TestStompSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.youtube.live.interaction.config.WebSocketConfig.Destinations.getChatLivestreamMessagesTopic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class LiveStreamingViewerCountPublisherTest extends WebSocketStompTest {

    @Autowired
    private LiveStreamingViewerCountPublisher sut;

    @Test
    @DisplayName("시청자 수는 스트리머를 제외하고 카운트된다")
    void viewerCountExcludesStreamer() throws ExecutionException, InterruptedException, TimeoutException {
        // given - 스트리머(user1)가 라이브 스트리밍 생성
        final LiveStreaming liveStreaming = createLiveStreaming("user1@example.com", "유저1", "password1!");

        final String streamerSessionId = TestAuthSupport.login("user1@example.com", "password1!");
        final TestStompSession<Integer> streamerSession = TestStompSession.connect(wsUrl, streamerSessionId);

        TestAuthSupport.signUp("user2@example.com", "유저2", "password2!");
        final String viewerSessionId = TestAuthSupport.login("user2@example.com", "password2!");
        final TestStompSession<Integer> viewerSession = TestStompSession.connect(wsUrl, viewerSessionId);

        final String countTopic = "/topic/livestreams/" + liveStreaming.getId() + "/viewer-count";
        streamerSession.subscribe(countTopic, Integer.class);

        // when - 스트리머만 구독
        streamerSession.subscribe(getChatLivestreamMessagesTopic(liveStreaming.getId()), Integer.class);
        sut.publishViewerCounts();

        // then - 스트리머는 카운트에서 제외되어 시청자 수 0
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<Integer> receivedMessages = streamerSession.getReceivedMessages(countTopic);
                    assertThat(receivedMessages).hasSize(1);
                    assertThat(receivedMessages.getFirst()).isEqualTo(0);
                });

        // when - 시청자 1명 추가 구독
        viewerSession.subscribe(getChatLivestreamMessagesTopic(liveStreaming.getId()), Integer.class);
        sut.publishViewerCounts();

        // then - 시청자 수 1로 증가 (스트리머 제외, 시청자만 카운트)
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<Integer> counts = streamerSession.getReceivedMessages(countTopic);
                    assertThat(counts.size()).isGreaterThan(1);
                    assertThat(counts.get(counts.size() - 1)).isEqualTo(1);
                });

        // when - 시청자 연결 해제
        viewerSession.disconnect();
        // WebSocketStompTest의 설정에서 스케쥴러 비활성화해서 브로드캐스트를 임의로 실행해야함.
        sut.publishViewerCounts();

        // then - 시청자 수 0으로 감소 (스트리머만 남음)
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<Integer> counts = streamerSession.getReceivedMessages(countTopic);
                    assertThat(counts.size()).isGreaterThan(2);
                    assertThat(counts.get(counts.size() - 1)).isEqualTo(0);
                });

        streamerSession.disconnect();
    }

    private LiveStreaming createLiveStreaming(final String email, final String username, final String password) {
        final Long userId = TestAuthSupport.signUp(email, username, password).as(Long.class);
        final Channel channel = testSupport.save(
                ChannelBuilder.Channel().withUser(UserBuilder.User().withId(userId).build()).build()
        );
        return testSupport.save(
                LiveStreamingBuilder.LiveStreaming().withChannel(channel).build()
        );
    }
}