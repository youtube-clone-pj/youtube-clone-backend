package com.youtube.live.interaction.livestreaming.domain;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LiveStreamingSubscriberManager {

    /**
     * 라이브 스트리밍별 시청자 추적 (카운팅용)
     *
     * 구조: Map<liveStreamingId, Map<viewerId, Set<simpSessionId>>>
     *
     * - liveStreamingId: 라이브 스트리밍 고유 ID
     * - viewerId: 고유 시청자 식별자 ("user:{userId}" 또는 "client:{clientId}")
     * - simpSessionId: WebSocket 세션 ID (탭/브라우저 창 단위)
     *
     * 설계 이유:
     * 1. 시청자 수는 고유 사용자(viewerId) 기준으로 카운트
     *    - 같은 사용자가 여러 탭을 열어도 시청자 수는 +1
     * 2. 각 탭(세션)은 개별적으로 추적 필요
     *    - 탭 하나만 닫아도 정확히 감지
     *    - 모든 탭을 닫았을 때만 viewerId 제거하여 시청자 수 -1
     *
     * 예시:
     * user:123이 3개 탭으로 liveStreaming=1을 시청하는 경우
     * {
     *   1L → {
     *     "user:123" → {"session-A", "session-B", "session-C"}
     *   }
     * }
     * → getSubscriberCount(1) = 1 (viewerId 개수, simpSessionId 개수가 아님)
     *
     * 탭 하나(session-B) 닫으면:
     * {
     *   1L → {
     *     "user:123" → {"session-A", "session-C"}
     *   }
     * }
     * → getSubscriberCount(1) = 1 (여전히 1명)
     *
     * 모든 탭 닫으면:
     * {
     *   1L → {}
     * }
     * → getSubscriberCount(1) = 0
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Set<String>>> liveStreamingToViewers = new ConcurrentHashMap<>();


    private record ViewerSession(Long liveStreamingId, String viewerId) {
        public boolean isDifferentLiveStreaming(final Long otherLiveStreamingId) {
            return !this.liveStreamingId.equals(otherLiveStreamingId);
        }
    }

    /**
     * 세션별 정보 추적 (Disconnect 시 O(1) 정리용)
     * Key: simpSessionId, Value: ViewerSession {liveStreamingId, viewerId}
     */
    private final ConcurrentHashMap<String, ViewerSession> sessionToViewerSession = new ConcurrentHashMap<>();

    private static final String USER_PREFIX = "user:";
    private static final String CLIENT_PREFIX = "client:";

    private String createViewerId(final Long userId, final String clientId) {
        return userId != null ? USER_PREFIX + userId : CLIENT_PREFIX + clientId;
    }

    public void addSubscriber(
            final Long liveStreamingId,
            final String simpSessionId,
            @Nullable final Long userId,
            @NonNull final String clientId
    ) {
        final String viewerId = createViewerId(userId, clientId);

        sessionToViewerSession.compute(simpSessionId, (key, oldSession) -> {
            // 1. 이전 라이브스트리밍에서 제거 (다른 라이브로 이동한 경우)
            if (oldSession != null && oldSession.isDifferentLiveStreaming(liveStreamingId)) {
                removeSessionFromLiveStreaming(
                        oldSession.liveStreamingId(),
                        oldSession.viewerId(),
                        simpSessionId
                );
            }

            // 2. 새 라이브스트리밍에 추가
            liveStreamingToViewers.compute(liveStreamingId, (id, viewers) -> {
                if (viewers == null) {
                    viewers = new ConcurrentHashMap<>();
                }

                viewers.compute(viewerId, (vId, sessions) -> {
                    if (sessions == null) {
                        sessions = ConcurrentHashMap.newKeySet();
                    }
                    sessions.add(simpSessionId);
                    return sessions;
                });

                return viewers;
            });

            // 3. 새로운 세션 정보 반환
            return new ViewerSession(liveStreamingId, viewerId);
        });
    }

    public void removeSubscriber(final String simpSessionId) {
        sessionToViewerSession.computeIfPresent(simpSessionId, (key, session) -> {
            removeSessionFromLiveStreaming(
                    session.liveStreamingId(),
                    session.viewerId(),
                    simpSessionId
            );

            return null;
        });
    }

    private void removeSessionFromLiveStreaming(
            final Long liveStreamingId,
            final String viewerId,
            final String simpSessionId
    ) {
        liveStreamingToViewers.computeIfPresent(liveStreamingId, (id, viewers) -> {
            viewers.computeIfPresent(viewerId, (vId, sessions) -> {
                sessions.remove(simpSessionId);
                // 마지막 세션이면 viewerId 자체를 제거
                return sessions.isEmpty() ? null : sessions;
            });
            // 시청자가 없으면 liveStreamingId 자체를 제거
            return viewers.isEmpty() ? null : viewers;
        });
    }

    public int getSubscriberCount(final Long liveStreamingId) {
        final ConcurrentHashMap<String, Set<String>> viewers =
                liveStreamingToViewers.get(liveStreamingId);

        return viewers == null ? 0 : viewers.size();
    }

    public Set<Long> getActiveLivestreamIds() {
        return Collections.unmodifiableSet(liveStreamingToViewers.keySet());
    }
}
