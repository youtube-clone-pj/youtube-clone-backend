package com.youtube.live.interaction.livestreaming.service;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.channel.domain.ChannelReader;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateRequest;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateResponse;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingViewerManager;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingWriter;
import com.youtube.live.interaction.livestreaming.event.LiveStreamingStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveStreamingService {

    private final LiveStreamingWriter liveStreamingWriter;
    private final ChannelReader channelReader;
    private final LiveStreamingViewerManager liveStreamingViewerManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LiveStreamingCreateResponse startLiveStreaming(
            final Long userId,
            final LiveStreamingCreateRequest request
    ) {
        final Channel channel = channelReader.readByUserId(userId);

        final LiveStreaming savedLiveStreaming = liveStreamingWriter.write(
                channel,
                request.title(),
                request.description(),
                request.thumbnailUrl(),
                LiveStreamingStatus.LIVE
        );

        liveStreamingViewerManager.registerStreamer(savedLiveStreaming.getId(), userId);

        eventPublisher.publishEvent(new LiveStreamingStartedEvent(
                savedLiveStreaming.getId(),
                savedLiveStreaming.getChannel().getId()
        ));

        return new LiveStreamingCreateResponse(
                savedLiveStreaming.getId(),
                savedLiveStreaming.getTitle(),
                savedLiveStreaming.getDescription(),
                savedLiveStreaming.getThumbnailUrl(),
                savedLiveStreaming.getStatus(),
                savedLiveStreaming.getChannel().getId()
        );
    }
}
