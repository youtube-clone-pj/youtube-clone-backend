package com.youtube.live.interaction.livestreaming.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LiveStreamingViewerManager {

    /**
     * 시청자 만료 TTL (Time To Live)
     *
     * 클라이언트 폴링 주기(20초)의 1.5배로 설정 (TTL = 폴링 주기 × 1.5)
     *
     * 폴링 주기보다 긴 이유:
     * 1. 네트워크 지연 허용: 정상 시청자가 일시적인 네트워크 지연으로 제외되는 것을 방지
     * 2. 일시적 연결 끊김 대응: 짧은 연결 문제 시에도 시청자 수가 안정적으로 유지
     *
     */
    private static final int VIEWER_TTL_SECONDS = 30;

    /**
     * 라이브 스트리밍별 시청자 목록
     * Key: livestreamId, Value: (viewerId -> lastSeen timestamp)
     *
     * 폴링 방식(V2)에서 시청자 수를 관리하기 위한 Heartbeat 기반 TTL 시스템
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Instant>> liveStreamViewers = new ConcurrentHashMap<>();

    /**
     * Heartbeat 기록 (하이브리드 방식)
     *
     * @param livestreamId 라이브 스트리밍 ID
     * @param clientId 클라이언트 고유 ID (서버 세션 기반)
     * @param userId 로그인한 사용자 ID (Optional)
     *
     * viewerId 우선순위: userId > clientId
     * - 로그인 유저: "user:{userId}" 형식으로 저장 (여러 탭 = 1명)
     * - 비로그인 유저: "client:{clientId}" 형식으로 저장 (여러 탭 = 1명)
     */
    public void recordHeartbeat(final Long livestreamId, final String clientId, final Long userId) {
        final String viewerId = userId != null
                ? "user:" + userId
                : "client:" + clientId;

        liveStreamViewers.compute(livestreamId, (id, viewers) -> {
            if (viewers == null) {
                viewers = new ConcurrentHashMap<>();
            }
            viewers.put(viewerId, Instant.now());
            return viewers;
        });

        log.debug("Heartbeat 기록 - livestreamId: {}, viewerId: {}", livestreamId, viewerId);
    }

    public int getViewerCount(final Long livestreamId) {
        cleanupExpiredViewers(livestreamId);
        return liveStreamViewers.getOrDefault(livestreamId, new ConcurrentHashMap<>()).size();
    }

    private void cleanupExpiredViewers(final Long livestreamId) {
        final Instant cutoffTime = Instant.now().minus(Duration.ofSeconds(VIEWER_TTL_SECONDS));

        liveStreamViewers.computeIfPresent(livestreamId, (id, viewers) -> {
            viewers.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoffTime));
            return viewers.isEmpty() ? null : viewers;
        });
    }

    /**
     * 스케줄러: 주기적으로 모든 라이브 스트리밍의 만료된 시청자 정리
     * 갑자기 종료된 라이브 스트리밍의 메모리 누수 방지
     */
    @Scheduled(fixedRate = 1800000) // 30분마다
    public void cleanupAllExpiredViewers() {
        liveStreamViewers.keySet().forEach(this::cleanupExpiredViewers);
    }
}
