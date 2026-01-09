package com.youtube.live.interaction.livestreaming.event;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatusCacheUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveStreamingStatusCacheEventListener {

    private final LiveStreamingStatusCacheUpdater cacheUpdater;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStatusChanged(final LiveStreamingStatusChangedEvent event) {
        cacheUpdater.updateCache(event.liveStreamingId(), event.status());
        log.debug("LiveStreaming 상태 캐시 업데이트 (트랜잭션 커밋 후) - liveStreamingId: {}, status: {}",
                event.liveStreamingId(), event.status());
    }
}
