package com.youtube.live.interaction.websocket.event;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingSubscriberManager;
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
 * 라이브 스트리밍 시청자 수 발행
 *
 * WebSocket 세션의 구독/연결해제 이벤트를 리스닝하고, 시청자 수를 주기적으로 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LiveStreamingViewerCountPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveStreamingSubscriberManager liveStreamingSubscriberManager;

    /**
     * 클라이언트가 특정 토픽을 구독할 때 호출
     */
    @EventListener
    public void handleSubscribe(final SessionSubscribeEvent event) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        final String destination = accessor.getDestination();
        final String simpSessionId = accessor.getSessionId();

        if (destination != null && destination.matches("/topic/livestreams/\\d+/chat/messages")) {
            liveStreamingSubscriberManager.addSubscriber(extractLivestreamId(destination), simpSessionId);
        }
    }

    /**
     * 클라이언트 연결이 끊어질 때 호출
     *
     * 정상 종료(DISCONNECT)와 비정상 종료(네트워크 끊김 등) 모두 처리됨
     */
    @EventListener
    public void handleDisconnect(final SessionDisconnectEvent event) {
        liveStreamingSubscriberManager.removeSubscriber(event.getSessionId());
    }

    //TODO TaskScheduler로 수정이 필요한 지 확인할 것
    @Scheduled(fixedRate = 5000)
    public void publishViewerCounts() {
        liveStreamingSubscriberManager.getActiveLivestreamIds().forEach(livestreamId -> {
            final int totalSubscribers = liveStreamingSubscriberManager.getSubscriberCount(livestreamId);
            final int viewerCount = Math.max(0, totalSubscribers - 1);

            messagingTemplate.convertAndSend(
                    "/topic/livestreams/" + livestreamId + "/viewer-count",
                    viewerCount
            );
        });
    }

    /**
     * destination에서 livestreamId를 추출
     *
     * @param destination 예: "/topic/livestreams/123/chat/messages"
     * @return livestreamId 예: 123
     */
    private Long extractLivestreamId(final String destination) {
        final String[] parts = destination.split("/");
        return Long.parseLong(parts[3]);
    }
}
