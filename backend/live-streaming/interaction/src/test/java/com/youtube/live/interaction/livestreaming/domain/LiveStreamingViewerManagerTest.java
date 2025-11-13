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
    @DisplayName("동일한 세션으로 중복 추가하면 시청자 수는 1로 유지된다")
    void addingSameSessionTwiceKeepsCountAsOne() {
        // given
        final Long livestreamId = 1L;
        final String simpSessionId = "session-1";

        // when
        sut.addViewer(livestreamId, simpSessionId);
        sut.addViewer(livestreamId, simpSessionId);

        // then
        assertThat(sut.getViewerCount(livestreamId)).isEqualTo(1);
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
    @DisplayName("동시에 시청자 추가와 제거가 발생해도 정확한 시청자 수를 유지한다")
    void addingAndRemovingViewersAtSameTimeKeepsAccurateCount() throws InterruptedException {
        // given
        final Long livestreamId = 1L;
        final int threadCount = 20;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount * 2);

        for (int i = 0; i < threadCount; i++) {
            final int sessionNumber = i;
            executorService.submit(() -> {
                try {
                    sut.addViewer(livestreamId, "session-" + sessionNumber);
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < threadCount / 2; i++) {
            final int sessionNumber = i;
            executorService.submit(() -> {
                try {
                    sut.removeViewer("session-" + sessionNumber);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(sut.getViewerCount(livestreamId)).isEqualTo(10);
    }

    @Test
    @DisplayName("여러 라이브 스트리밍에 동시에 시청자가 추가되어도 각 라이브 스트리밍의 시청자 수를 정확히 유지한다")
    void addingViewersToMultipleLivestreamsAtSameTimeKeepsAccurateCount() throws InterruptedException {
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
                        sut.addViewer(finalLivestreamId, "session-livestream" + finalLivestreamId + "-viewer" + finalViewerIndex);
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
            assertThat(sut.getViewerCount((long) livestreamId)).isEqualTo(viewersPerLivestream);
        }
        assertThat(sut.getActiveLivestreamIds()).hasSize(livestreamCount);
    }
}
