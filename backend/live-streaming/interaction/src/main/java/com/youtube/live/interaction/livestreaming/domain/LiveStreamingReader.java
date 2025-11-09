package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.live.interaction.livestreaming.repository.LiveStreamingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveStreamingReader {

    private final LiveStreamingRepository liveStreamingRepository;

    public LiveStreaming readBy(final Long liveStreamingId) {
        return liveStreamingRepository.findById(liveStreamingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 라이브 스트리밍입니다"));
    }
}
