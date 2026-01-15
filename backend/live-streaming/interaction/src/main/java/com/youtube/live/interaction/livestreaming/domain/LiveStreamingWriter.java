package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.core.channel.domain.Channel;
import com.youtube.live.interaction.livestreaming.repository.LiveStreamingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveStreamingWriter {

    private final LiveStreamingRepository liveStreamingRepository;

    public LiveStreaming write(
            final Channel channel,
            final String title,
            final String description,
            final String thumbnailUrl,
            final LiveStreamingStatus status
    ) {
        final LiveStreaming liveStreaming = LiveStreaming.builder()
                .channel(channel)
                .title(title)
                .description(description)
                .thumbnailUrl(thumbnailUrl)
                .status(status)
                .build();

        log.info("LiveStreaming 생성 - liveStreamingId: {}, channelId: {}",
                liveStreaming.getId(), liveStreaming.getChannel().getId());

        return liveStreamingRepository.save(liveStreaming);
    }

    @Transactional
    public void updateStatus(final LiveStreaming liveStreaming, final LiveStreamingStatus newStatus) {
        liveStreaming.updateStatus(newStatus);
        log.info("LiveStreaming 상태 변경 - liveStreamingId: {}, newStatus: {}",
                liveStreaming.getId(), newStatus);
    }
}
