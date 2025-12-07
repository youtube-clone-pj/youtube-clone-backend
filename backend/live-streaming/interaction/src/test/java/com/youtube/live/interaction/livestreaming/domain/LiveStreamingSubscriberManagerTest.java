package com.youtube.live.interaction.livestreaming.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LiveStreamingSubscriberManagerTest {

    private LiveStreamingSubscriberManager sut;

    @BeforeEach
    void setUp() {
        sut = new LiveStreamingSubscriberManager();
    }

    @Test
    @DisplayName("로그인 사용자가 여러 탭에서 접속해도 시청자 수는 1로 유지된다")
    void loggedInUserWithMultipleTabsCountedAsOne() {
        // given
        final Long livestreamId = 1L;
        final Long userId = 100L;
        final String clientId = "client-1";

        // when
        sut.addSubscriber(livestreamId, "session-1", userId, clientId);
        sut.addSubscriber(livestreamId, "session-2", userId, clientId);
        sut.addSubscriber(livestreamId, "session-3", userId, clientId);

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(1);
    }

    @Test
    @DisplayName("로그인 사용자가 탭 1개를 닫아도 다른 탭이 남아있으면 시청자 수는 1로 유지된다")
    void loggedInUserClosingOneTabKeepsCountAsOne() {
        // given
        final Long livestreamId = 1L;
        final Long userId = 100L;
        final String clientId = "client-1";

        sut.addSubscriber(livestreamId, "session-1", userId, clientId);
        sut.addSubscriber(livestreamId, "session-2", userId, clientId);
        sut.addSubscriber(livestreamId, "session-3", userId, clientId);

        // when
        sut.removeSubscriber("session-1");

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(1);
    }

    @Test
    @DisplayName("로그인 사용자가 모든 탭을 닫으면 시청자 수는 0으로 감소한다")
    void loggedInUserClosingAllTabsDecreasesCountToZero() {
        // given
        final Long livestreamId = 1L;
        final Long userId = 100L;
        final String clientId = "client-1";

        sut.addSubscriber(livestreamId, "session-1", userId, clientId);
        sut.addSubscriber(livestreamId, "session-2", userId, clientId);
        sut.addSubscriber(livestreamId, "session-3", userId, clientId);

        // when
        sut.removeSubscriber("session-1");
        sut.removeSubscriber("session-2");
        sut.removeSubscriber("session-3");

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isZero();
    }

    @Test
    @DisplayName("비로그인 사용자가 여러 탭에서 접속해도 시청자 수는 1로 유지된다")
    void guestUserWithMultipleTabsCountedAsOne() {
        // given
        final Long livestreamId = 1L;
        final String clientId = "client-1";

        // when
        sut.addSubscriber(livestreamId, "session-1", null, clientId);
        sut.addSubscriber(livestreamId, "session-2", null, clientId);

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 사용자는 각각 별도로 카운팅된다")
    void differentUsersCountedSeparately() {
        // given
        final Long livestreamId = 1L;

        // when
        // 사용자 A (로그인)
        sut.addSubscriber(livestreamId, "session-1", 100L, "client-1");
        // 사용자 B (로그인)
        sut.addSubscriber(livestreamId, "session-2", 200L, "client-2");
        // 사용자 C (비로그인)
        sut.addSubscriber(livestreamId, "session-3", null, "client-3");

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(3);
    }

    @Test
    @DisplayName("사용자가 다른 라이브 스트리밍으로 이동하면 이전 라이브에서 제거된다")
    void userMovingToAnotherLivestreamRemovesFromPrevious() {
        // given
        final Long livestreamId1 = 1L;
        final Long livestreamId2 = 2L;
        final Long userId = 100L;
        final String clientId = "client-1";

        sut.addSubscriber(livestreamId1, "session-1", userId, clientId);

        // when
        sut.addSubscriber(livestreamId2, "session-1", userId, clientId);

        // then
        assertThat(sut.getSubscriberCount(livestreamId1)).isZero();
        assertThat(sut.getSubscriberCount(livestreamId2)).isEqualTo(1);
    }

    @Test
    @DisplayName("동일한 세션으로 중복 추가하면 시청자 수는 1로 유지된다")
    void addingSameSessionTwiceKeepsCountAsOne() {
        // given
        final Long livestreamId = 1L;
        final String simpSessionId = "session-1";
        final Long userId = 100L;
        final String clientId = "client-1";

        // when
        sut.addSubscriber(livestreamId, simpSessionId, userId, clientId);
        sut.addSubscriber(livestreamId, simpSessionId, userId, clientId);

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(1);
    }

    @Test
    @DisplayName("시청자가 없는 라이브 스트리밍의 시청자 수는 0이다")
    void emptyLivestreamHasZeroSubscribers() {
        // given
        final Long livestreamId = 1L;

        // when
        final int count = sut.getSubscriberCount(livestreamId);

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("동시에 시청자 추가와 제거가 발생해도 정확한 시청자 수를 유지한다")
    void addingAndRemovingSubscribersAtSameTimeKeepsAccurateCount() throws InterruptedException {
        // given
        final Long livestreamId = 1L;
        final int threadCount = 20;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount * 2);

        for (int i = 0; i < threadCount; i++) {
            final int sessionNumber = i;
            executorService.submit(() -> {
                try {
                    sut.addSubscriber(
                            livestreamId,
                            "session-" + sessionNumber,
                            (long) sessionNumber,
                            "client-" + sessionNumber
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < threadCount / 2; i++) {
            final int sessionNumber = i;
            executorService.submit(() -> {
                try {
                    sut.removeSubscriber("session-" + sessionNumber);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(10);
    }

    @Test
    @DisplayName("여러 라이브 스트리밍에 동시에 시청자가 추가되어도 각 라이브 스트리밍의 시청자 수를 정확히 유지한다")
    void addingSubscribersToMultipleLivestreamsAtSameTimeKeepsAccurateCount() throws InterruptedException {
        // given
        final int livestreamCount = 10;
        final int viewersPerLivestream = 50;
        final int totalThreads = livestreamCount * viewersPerLivestream;
        final ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        final CountDownLatch latch = new CountDownLatch(totalThreads);

        // when
        for (int livestreamId = 1; livestreamId <= livestreamCount; livestreamId++) {
            final long finalLivestreamId = livestreamId;
            for (int viewerIndex = 0; viewerIndex < viewersPerLivestream; viewerIndex++) {
                final int finalViewerIndex = viewerIndex;
                executorService.submit(() -> {
                    try {
                        sut.addSubscriber(
                                finalLivestreamId,
                                "session-livestream" + finalLivestreamId + "-viewer" + finalViewerIndex,
                                (long) finalViewerIndex,
                                "client-livestream" + finalLivestreamId + "-viewer" + finalViewerIndex
                        );
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        for (int livestreamId = 1; livestreamId <= livestreamCount; livestreamId++) {
            assertThat(sut.getSubscriberCount((long) livestreamId)).isEqualTo(viewersPerLivestream);
        }
        assertThat(sut.getActiveLivestreamIds()).hasSize(livestreamCount);
    }

    @Test
    @DisplayName("스트리머가 등록되지 않은 경우 전체 시청자 수를 반환한다")
    void getSubscriberCountWithNoStreamerReturnsFullCount() {
        // given
        final Long livestreamId = 1L;
        final Long userId1 = 1L;
        final Long userId2 = 2L;

        // when
        sut.addSubscriber(livestreamId, "session-1", userId1, "client-1");
        sut.addSubscriber(livestreamId, "session-2", userId2, "client-2");

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(2);
    }

    @Test
    @DisplayName("스트리머가 등록되었지만 구독하지 않은 경우 전체 시청자 수를 반환한다")
    void getSubscriberCountWhenStreamerNotSubscribedReturnsFullCount() {
        // given
        final Long livestreamId = 1L;
        final Long streamerUserId = 999L;
        final Long userId1 = 1L;
        final Long userId2 = 2L;

        sut.registerStreamer(livestreamId, streamerUserId);

        // when
        sut.addSubscriber(livestreamId, "session-1", userId1, "client-1");
        sut.addSubscriber(livestreamId, "session-2", userId2, "client-2");

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(2);
    }

    @Test
    @DisplayName("스트리머만 구독한 경우 시청자 수는 0이다")
    void getSubscriberCountWhenOnlyStreamerSubscribedReturnsZero() {
        // given
        final Long livestreamId = 1L;
        final Long streamerUserId = 1L;

        sut.registerStreamer(livestreamId, streamerUserId);

        // when
        sut.addSubscriber(livestreamId, "session-streamer", streamerUserId, "client-streamer");

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(0);
    }

    @Test
    @DisplayName("스트리머와 시청자가 함께 구독한 경우 스트리머를 제외하고 카운트된다")
    void getSubscriberCountWithStreamerAndViewersExcludesStreamer() {
        // given
        final Long livestreamId = 1L;
        final Long streamerUserId = 1L;
        final Long userId1 = 2L;
        final Long userId2 = 3L;

        sut.registerStreamer(livestreamId, streamerUserId);

        // when
        sut.addSubscriber(livestreamId, "session-streamer", streamerUserId, "client-streamer");
        sut.addSubscriber(livestreamId, "session-1", userId1, "client-1");
        sut.addSubscriber(livestreamId, "session-2", userId2, "client-2");

        // then
        assertThat(sut.getSubscriberCount(livestreamId)).isEqualTo(2);
    }
}
