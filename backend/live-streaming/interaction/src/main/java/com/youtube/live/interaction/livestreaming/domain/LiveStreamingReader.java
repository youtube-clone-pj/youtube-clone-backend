package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.common.exception.BaseException;
import com.youtube.live.interaction.exception.LiveStreamingErrorCode;
import com.youtube.live.interaction.livestreaming.repository.LiveStreamingRepository;
import com.youtube.live.interaction.livestreaming.repository.dto.LiveStreamingMetadataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveStreamingReader {

    private final LiveStreamingRepository liveStreamingRepository;

    public LiveStreaming readBy(final Long liveStreamingId) {
        return liveStreamingRepository.findById(liveStreamingId)
                .orElseThrow(() -> new BaseException(LiveStreamingErrorCode.LIVE_STREAMING_NOT_FOUND));
    }

    public LiveStreaming getReferenceBy(final Long liveStreamingId) {
        return liveStreamingRepository.getReferenceById(liveStreamingId);
    }

    @Cacheable(value = "liveStreamingStatus", key = "#liveStreamingId", sync = true)
    public LiveStreamingStatus readCachedStatusBy(final Long liveStreamingId) {
        return liveStreamingRepository.findStatusById(liveStreamingId)
                .orElseThrow(() -> new BaseException(LiveStreamingErrorCode.LIVE_STREAMING_NOT_FOUND));
    }

    public LiveStreamingMetadataResponse readMetadataBy(final Long liveStreamingId) {
        return liveStreamingRepository.findMetadataById(liveStreamingId);
    }
}
