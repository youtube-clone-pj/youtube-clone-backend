package com.youtube.live.interaction.config;

import com.youtube.api.testfixtures.support.TestAuthSupport;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.testfixtures.builder.ChannelBuilder;
import com.youtube.core.testfixtures.builder.UserBuilder;
import com.youtube.live.interaction.builder.LiveStreamingBuilder;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.support.TestStompSession;
import com.youtube.live.interaction.websocket.event.LiveStreamingViewerCountBroadcaster;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.youtube.live.interaction.config.WebSocketConfig.Destinations.getRoomTopic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class LiveStreamingViewerCountBroadcasterTest extends WebSocketStompTest {

    @Autowired
    private LiveStreamingViewerCountBroadcaster sut;

    @Test
    @DisplayName("클라이언트 구독 및 해제 시 시청자 수 변경 이벤트가 정상 동작한다")
    void subscribeAndUnsubscribeEventsWorkCorrectly() throws ExecutionException, InterruptedException, TimeoutException {
        // given
        final LiveStreaming liveStreaming = createLiveStreaming("user1@example.com", "유저1", "password1!");

        final String jsessionId1 = TestAuthSupport.login("user1@example.com", "password1!");
        final TestStompSession<Integer> session1 = TestStompSession.connect(wsUrl, jsessionId1);

        TestAuthSupport.signUp("user2@example.com", "유저2", "password2!");
        final String jsessionId2 = TestAuthSupport.login("user2@example.com", "password2!");
        final TestStompSession<Integer> session2 = TestStompSession.connect(wsUrl, jsessionId2);

        final String countTopic = "/topic/room/" + liveStreaming.getId() + "/count";
        session1.subscribe(countTopic, Integer.class);

        // when - 두 클라이언트가 방 구독
        session1.subscribe(getRoomTopic(liveStreaming.getId()), Integer.class);
        session2.subscribe(getRoomTopic(liveStreaming.getId()), Integer.class);

        sut.broadcastViewerCounts();

        // then - 시청자 수 2가 브로드캐스트됨
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    final List<Integer> counts = session1.getReceivedMessages(countTopic);
                    assertThat(counts).hasSize(1);
                    assertThat(counts.getFirst()).isEqualTo(2);
                });

        // when - 한 클라이언트 연결 해제
        session2.disconnect();
        // WebSocketStompTest의 설정에서 스케쥴러 비활성화해서 브로드캐스트를 임의로 실행해야함.
        sut.broadcastViewerCounts();

        // then - 연결 해제가 완전히 처리되고 시청자 수 1로 감소
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    final List<Integer> counts = session1.getReceivedMessages(countTopic);
                    assertThat(counts.size()).isGreaterThan(1);
                    assertThat(counts.get(counts.size() - 1)).isEqualTo(1);
                });

        session1.disconnect();
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