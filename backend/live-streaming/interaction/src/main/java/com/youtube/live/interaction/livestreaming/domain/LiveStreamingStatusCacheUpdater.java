package com.youtube.live.interaction.livestreaming.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveStreamingStatusCacheUpdater {

    @CachePut(value = "liveStreamingStatus", key = "#liveStreamingId")
    public LiveStreamingStatus updateCache(final Long liveStreamingId, final LiveStreamingStatus status) {
        return status;
    }
}
