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
        final Long roomId = 1L;
        final String simpSessionId = "session-1";

        // when
        sut.addViewer(roomId, simpSessionId);
        sut.addViewer(roomId, simpSessionId);

        // then
        assertThat(sut.getViewerCount(roomId)).isEqualTo(1);
    }

    @Test
    @DisplayName("시청자가 없는 방의 시청자 수는 0이다")
    void emptyRoomHasZeroViewers() {
        // given
        final Long roomId = 1L;

        // when
        final int count = sut.getViewerCount(roomId);

        // then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("동시에 시청자 추가와 제거가 발생해도 정확한 시청자 수를 유지한다")
    void addingAndRemovingViewersAtSameTimeKeepsAccurateCount() throws InterruptedException {
        // given
        final Long roomId = 1L;
        final int threadCount = 20;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount * 2);

        for (int i = 0; i < threadCount; i++) {
            final int sessionNumber = i;
            executorService.submit(() -> {
                try {
                    sut.addViewer(roomId, "session-" + sessionNumber);
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
        assertThat(sut.getViewerCount(roomId)).isEqualTo(10);
    }

    @Test
    @DisplayName("여러 방에 동시에 시청자가 추가되어도 각 방의 시청자 수를 정확히 유지한다")
    void addingViewersToMultipleRoomsAtSameTimeKeepsAccurateCount() throws InterruptedException {
        // given
        final int roomCount = 10;
        final int viewersPerRoom = 50;
        final int totalThreads = roomCount * viewersPerRoom;
        final ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        final CountDownLatch latch = new CountDownLatch(totalThreads);

        // when
        for (int roomId = 1; roomId <= roomCount; roomId++) {
            final long finalRoomId = roomId;
            for (int viewerIndex = 0; viewerIndex < viewersPerRoom; viewerIndex++) {
                final int finalViewerIndex = viewerIndex;
                executorService.submit(() -> {
                    try {
                        sut.addViewer(finalRoomId, "session-room" + finalRoomId + "-viewer" + finalViewerIndex);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        for (int roomId = 1; roomId <= roomCount; roomId++) {
            assertThat(sut.getViewerCount((long) roomId)).isEqualTo(viewersPerRoom);
        }
        assertThat(sut.getActiveRoomIds()).hasSize(roomCount);
    }


    @Test
    @DisplayName("마지막 시청자가 나가는 동시에 새 시청자가 입장하면 새 시청자가 유실되지 않는다")
    void lastViewerLeavingWhileNewViewerJoiningDoesNotLoseData() throws InterruptedException {
        // given
        final Long roomId = 1L;
        final int iterations = 100;
        int dataLossCount = 0;

        // when: 반복적으로 race condition 발생 시도
        for (int i = 0; i < iterations; i++) {
            final String existingSession = "session-existing-" + i;
            final String newSession = "session-new-" + i;

            // 기존 시청자 1명 추가
            sut.addViewer(roomId, existingSession);

            final ExecutorService executorService = Executors.newFixedThreadPool(2);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch endLatch = new CountDownLatch(2);

            // 스레드1: 마지막 시청자 제거
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    sut.removeViewer(existingSession);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });

            // 스레드2: 새 시청자 추가
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    sut.addViewer(roomId, newSession);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });

            startLatch.countDown(); // 동시 시작
            endLatch.await(1, TimeUnit.SECONDS);
            executorService.shutdown();

            // 검증: 새 시청자가 추가되었어야 함
            final int viewerCount = sut.getViewerCount(roomId);
            if (viewerCount == 0) {
                dataLossCount++;
            }

            // 다음 iteration을 위한 정리
            sut.removeViewer(newSession);
        }

        // then: 데이터 손실이 발생하지 않아야 함
        assertThat(dataLossCount)
                .as("데이터 손실이 %d번 발생했습니다", dataLossCount)
                .isZero();
    }

}
