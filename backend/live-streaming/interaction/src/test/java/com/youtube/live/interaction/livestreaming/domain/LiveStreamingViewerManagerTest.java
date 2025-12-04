package com.youtube.live.interaction.livestreaming.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LiveStreamingViewerManagerTest {

    private LiveStreamingViewerManager sut;

    @BeforeEach
    void setUp() {
        sut = new LiveStreamingViewerManager();
    }

    @Test
    @DisplayName("비로그인 사용자가 여러 번 heartbeat를 기록하면 시청자 수는 1로 유지된다")
    void recordingHeartbeatForSameAnonymousUserKeepsCountAsOne() {
        // given
        final Long livestreamId = 1L;
        final String clientId = "client-100";

        // when
        sut.recordHeartbeat(livestreamId, clientId, null);
        sut.recordHeartbeat(livestreamId, clientId, null);

        // then
        assertThat(sut.getViewerCount(livestreamId)).isEqualTo(1);
    }

    @Test
    @DisplayName("로그인 사용자가 여러 탭에서 접속해도 시청자 수는 1로 유지된다")
    void recordingHeartbeatForSameLoggedInUserKeepsCountAsOne() {
        // given
        final Long livestreamId = 1L;
        final String clientId1 = "client-100";
        final String clientId2 = "client-200";
        final Long userId = 1L;

        // when
        sut.recordHeartbeat(livestreamId, clientId1, userId);
        sut.recordHeartbeat(livestreamId, clientId2, userId);

        // then
        assertThat(sut.getViewerCount(livestreamId)).isEqualTo(1);
    }

    @Test
    @DisplayName("비로그인 사용자가 로그인하면 일시적으로 2명으로 카운팅되며 TTL 이후 1명으로 수렴한다")
    void recordingHeartbeatBeforeAndAfterLoginTemporarilyCountsAsTwo() {
        // given
        final Long livestreamId = 1L;
        final String clientId = "client-100";
        final Long userId = 1L;

        // when: 비로그인으로 시청
        sut.recordHeartbeat(livestreamId, clientId, null);
        assertThat(sut.getViewerCount(livestreamId)).isEqualTo(1);

        // when: 로그인 후 시청
        // clientId는 세션에 유지되지만, viewerId는 "client:xxx"에서 "user:xxx"로 변경됨
        sut.recordHeartbeat(livestreamId, clientId, userId);

        // then: 로그인 직후에는 일시적으로 2명으로 카운팅됨 (제한사항)
        // 비로그인 "client:xxx"는 30초 후 TTL로 자동 제거되어 최종적으로 1명으로 수렴
        assertThat(sut.getViewerCount(livestreamId)).isEqualTo(2);
    }

    @Test
    @DisplayName("시청자가 없는 라이브 스트리밍의 시청자 수는 0이다")
    void emptyLivestreamHasZeroViewers() {
        // given
        final Long livestreamId = 1L;

        // when
        final int count = sut.getViewerCount(livestreamId);

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("동시에 여러 시청자가 heartbeat를 기록해도 정확한 시청자 수를 유지한다")
    void concurrentHeartbeatsKeepAccurateViewerCount() throws InterruptedException {
        // given
        final Long livestreamId = 1L;
        final int viewerCount = 50;
        final ExecutorService executorService = Executors.newFixedThreadPool(viewerCount);
        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch doneSignal = new CountDownLatch(viewerCount);

        // when
        for (int i = 0; i < viewerCount; i++) {
            final String clientId = "client-" + i;
            executorService.submit(() -> {
                try {
                    startSignal.await();
                    sut.recordHeartbeat(livestreamId, clientId, null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown();
        doneSignal.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(sut.getViewerCount(livestreamId)).isEqualTo(50);
    }

    @Test
    @DisplayName("여러 라이브 스트리밍에 동시에 시청자가 추가되어도 각 라이브 스트리밍의 시청자 수를 정확히 유지한다")
    void addingViewersToMultipleLivestreamsAtSameTimeKeepsAccurateCount() throws InterruptedException {
        // given
        final int livestreamCount = 10;
        final int viewersPerLivestream = 50;
        final int totalThreads = livestreamCount * viewersPerLivestream;
        final ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch doneSignal = new CountDownLatch(totalThreads);

        // when
        for (int livestreamId = 1; livestreamId <= livestreamCount; livestreamId++) {
            final long finalLivestreamId = livestreamId;
            for (int viewerIndex = 0; viewerIndex < viewersPerLivestream; viewerIndex++) {
                final String clientId = "client-" + viewerIndex;
                executorService.submit(() -> {
                    try {
                        startSignal.await();
                        sut.recordHeartbeat(finalLivestreamId, clientId, null);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneSignal.countDown();
                    }
                });
            }
        }

        startSignal.countDown();
        doneSignal.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        for (int livestreamId = 1; livestreamId <= livestreamCount; livestreamId++) {
            assertThat(sut.getViewerCount((long) livestreamId)).isEqualTo(50);
        }
    }
}
