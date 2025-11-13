package com.youtube.live.interaction.livestreaming.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LiveStreamingViewerManager {

    /**
     * 라이브 스트리밍별 시청자 세션 목록
     * Key: livestreamId, Value: 해당 라이브 스트리밍을 구독 중인 simpSessionId 집합
     *
     * 시청자 수 계산
     */
    private final ConcurrentHashMap<Long, Set<String>> livestreamViewers = new ConcurrentHashMap<>();

    /**
     * 세션별 구독 중인 라이브 스트리밍
     * Key: simpSessionId, Value: 구독 중인 livestreamId
     *
     * 연결 해제 시 어떤 라이브 스트리밍의 카운트를 감소시켜야 하는지 빠르게 찾기 위해 사용
     */
    private final ConcurrentHashMap<String, Long> sessionLivestream = new ConcurrentHashMap<>();

    public void addViewer(final Long livestreamId, final String simpSessionId) {
        sessionLivestream.compute(simpSessionId, (sid, prevLivestreamId) -> {
            // 1) 이전 라이브 스트리밍에서 제거 (다르면)
            if (prevLivestreamId != null && !prevLivestreamId.equals(livestreamId)) {
                livestreamViewers.computeIfPresent(prevLivestreamId, (k, viewers) -> {
                    viewers.remove(simpSessionId);
                    return viewers.isEmpty() ? null : viewers;
                });
            }
            // 2) 새 라이브 스트리밍에 추가
            livestreamViewers.compute(livestreamId, (k, viewers) -> {
                if (viewers == null) viewers = ConcurrentHashMap.newKeySet();
                viewers.add(simpSessionId);
                return viewers;
            });
            // 3) 매핑 최신화
            return livestreamId;
        });
    }

    public void removeViewer(final String simpSessionId) {
        sessionLivestream.computeIfPresent(simpSessionId, (sid, livestreamId) -> {
            // 라이브 스트리밍의 viewer 목록에서도 세션 제거
            livestreamViewers.computeIfPresent(livestreamId, (k, viewers) -> {
                viewers.remove(simpSessionId);
                // 라이브 스트리밍에 시청자가 더 이상 없으면 맵에서 정리
                return viewers.isEmpty() ? null : viewers;
            });
            // sessionLivestream에서 제거 (null 반환)
            return null;
        });
    }

    public int getViewerCount(final Long livestreamId) {
        return livestreamViewers.getOrDefault(livestreamId, Collections.emptySet()).size();
    }

    public Set<Long> getActiveLivestreamIds() {
        return Collections.unmodifiableSet(livestreamViewers.keySet());
    }
}
