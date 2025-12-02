package com.youtube.live.interaction.livestreaming.service;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingReader;
import com.youtube.live.interaction.livestreaming.repository.dto.LiveStreamingMetadataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveStreamingQueryService {

    private final LiveStreamingReader liveStreamingReader;

    public LiveStreamingMetadataResponse getMetadata(final Long liveStreamingId) {
        return liveStreamingReader.readMetadataBy(liveStreamingId);
    }
}
