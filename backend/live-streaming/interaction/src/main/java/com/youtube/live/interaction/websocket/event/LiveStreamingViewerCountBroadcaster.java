package com.youtube.live.interaction.websocket.event;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingViewerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * 라이브 스트리밍 실시간 시청자 수 관리
 *
 * WebSocket 세션의 구독/연결해제 이벤트를 리스닝
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LiveStreamingViewerCountBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveStreamingViewerManager liveStreamingViewerManager;

    /**
     * 클라이언트가 특정 토픽을 구독할 때 호출
     */
    @EventListener
    public void handleSubscribe(final SessionSubscribeEvent event) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        final String destination = accessor.getDestination();
        final String simpSessionId = accessor.getSessionId();

        if (destination != null && destination.matches("/topic/room/\\d+")) {
            liveStreamingViewerManager.addViewer(extractRoomId(destination), simpSessionId);
        }
    }

    /**
     * 클라이언트 연결이 끊어질 때 호출
     *
     * 정상 종료(DISCONNECT)와 비정상 종료(네트워크 끊김 등) 모두 처리됨
     */
    @EventListener
    public void handleDisconnect(final SessionDisconnectEvent event) {
        liveStreamingViewerManager.removeViewer(event.getSessionId());
    }

    @Scheduled(fixedRate = 5000)
    public void broadcastViewerCounts() {
        liveStreamingViewerManager.getActiveRoomIds().forEach(roomId -> {
            final int count = liveStreamingViewerManager.getViewerCount(roomId);

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId + "/count",
                    count
            );
        });
    }

    /**
     * destination에서 roomId를 추출
     *
     * @param destination 예: "/topic/room/123"
     * @return roomId 예: 123
     */
    private Long extractRoomId(final String destination) {
        final String[] parts = destination.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }
}
