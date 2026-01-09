package com.youtube.live.interaction.livestreaming.service;

import com.youtube.common.exception.BaseException;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.channel.domain.ChannelReader;
import com.youtube.live.interaction.exception.LiveStreamingErrorCode;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateRequest;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateResponse;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingReader;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingSubscriberManager;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingViewerManager;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingWriter;
import com.youtube.live.interaction.livestreaming.event.LiveStreamingStartedEvent;
import com.youtube.live.interaction.livestreaming.event.LiveStreamingStatusChangedEvent;
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
    private final LiveStreamingReader liveStreamingReader;
    private final ChannelReader channelReader;
    private final LiveStreamingSubscriberManager liveStreamingSubscriberManager;
    private final LiveStreamingViewerManager liveStreamingViewerManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LiveStreamingCreateResponse startLiveStreamingV1(
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

        liveStreamingSubscriberManager.registerStreamer(savedLiveStreaming.getId(), userId);

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

    @Transactional
    public LiveStreamingCreateResponse startLiveStreamingV2(
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

        eventPublisher.publishEvent(new LiveStreamingStatusChangedEvent(
                savedLiveStreaming.getId(),
                LiveStreamingStatus.LIVE
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

    @Transactional
    public void endLiveStreaming(final Long liveStreamingId, final Long userId) {
        final LiveStreaming liveStreaming = liveStreamingReader.readBy(liveStreamingId);
        final Channel channel = liveStreaming.getChannel();

        if (!channel.isOwnedBy(userId)) {
            throw new BaseException(LiveStreamingErrorCode.NOT_OWNER_OF_LIVE_STREAMING);
        }

        liveStreamingWriter.updateStatus(liveStreaming, LiveStreamingStatus.ENDED);

        liveStreamingSubscriberManager.unregisterStreamer(liveStreamingId);
        liveStreamingViewerManager.endLiveStreaming(liveStreamingId);

        eventPublisher.publishEvent(new LiveStreamingStatusChangedEvent(
                liveStreamingId,
                LiveStreamingStatus.ENDED
        ));

        log.info("LiveStreaming 종료 - liveStreamingId: {}, userId: {}", liveStreamingId, userId);
    }
}
