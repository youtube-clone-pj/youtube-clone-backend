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
     * 방별 시청자 세션 목록
     * Key: roomId, Value: 해당 방을 구독 중인 simpSessionId 집합
     *
     * 시청자 수 계산
     */
    private final ConcurrentHashMap<Long, Set<String>> roomViewers = new ConcurrentHashMap<>();

    /**
     * 세션별 구독 중인 방
     * Key: simpSessionId, Value: 구독 중인 roomId
     *
     * 연결 해제 시 어떤 방의 카운트를 감소시켜야 하는지 빠르게 찾기 위해 사용
     */
    private final ConcurrentHashMap<String, Long> sessionRoom = new ConcurrentHashMap<>();

    public void addViewer(final Long roomId, final String simpSessionId) {
        sessionRoom.compute(simpSessionId, (sid, prevRoomId) -> {
            // 1) 이전 방에서 제거 (다르면)
            if (prevRoomId != null && !prevRoomId.equals(roomId)) {
                roomViewers.computeIfPresent(prevRoomId, (k, viewers) -> {
                    viewers.remove(simpSessionId);
                    return viewers.isEmpty() ? null : viewers;
                });
            }
            // 2) 새 방에 추가
            roomViewers.compute(roomId, (k, viewers) -> {
                if (viewers == null) viewers = ConcurrentHashMap.newKeySet();
                viewers.add(simpSessionId);
                return viewers;
            });
            // 3) 매핑 최신화
            return roomId;
        });
    }

    public void removeViewer(final String simpSessionId) {
        sessionRoom.computeIfPresent(simpSessionId, (sid, roomId) -> {
            // 방의 viewer 목록에서도 세션 제거
            roomViewers.computeIfPresent(roomId, (k, viewers) -> {
                viewers.remove(simpSessionId);
                // 방에 시청자가 더 이상 없으면 맵에서 정리
                return viewers.isEmpty() ? null : viewers;
            });
            // sessionRoom에서 제거 (null 반환)
            return null;
        });
    }

    public int getViewerCount(final Long roomId) {
        return roomViewers.getOrDefault(roomId, Collections.emptySet()).size();
    }

    public Set<Long> getActiveRoomIds() {
        return Collections.unmodifiableSet(roomViewers.keySet());
    }
}
