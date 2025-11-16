package com.youtube.live.interaction.websocket.event;

import com.youtube.live.interaction.livestreaming.domain.ReactionReader;
import com.youtube.live.interaction.livestreaming.domain.ReactionType;
import com.youtube.live.interaction.websocket.event.dto.LikeCountBroadcastResponse;
import com.youtube.live.interaction.websocket.event.dto.ReactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 라이브 스트리밍 좋아요 카운트 발행
 * <p>
 * 좋아요 이벤트를 리스닝하고, 좋아요 카운트를 WebSocket으로 브로드캐스트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LiveStreamingReactionCountPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final ReactionReader reactionReader;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onReactionEvent(final ReactionEvent event) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/livestreams/" + event.liveStreamingId() + "/like-count",
                    new LikeCountBroadcastResponse(reactionReader.countBy(event.liveStreamingId(), ReactionType.LIKE))
            );
        } catch (Exception e) {
            log.warn("좋아요 카운트 브로드캐스트 실패 - liveStreamingId: {}, error: {}",
                    event.liveStreamingId(), e.getMessage(), e);
        }
    }
}
