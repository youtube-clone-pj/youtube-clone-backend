package com.youtube.live.interaction.livestreaming.service;

import com.youtube.live.interaction.livestreaming.service.dto.LiveStatsResponse;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingReader;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingViewerManager;
import com.youtube.live.interaction.livestreaming.domain.ReactionReader;
import com.youtube.live.interaction.livestreaming.domain.ReactionType;
import com.youtube.live.interaction.livestreaming.repository.dto.LiveStreamingMetadataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveStreamingQueryService {

    private final LiveStreamingReader liveStreamingReader;
    private final LiveStreamingViewerManager liveStreamingViewerManager;
    private final ReactionReader reactionReader;

    public LiveStreamingMetadataResponse getMetadata(final Long liveStreamingId) {
        return liveStreamingReader.readMetadataBy(liveStreamingId);
    }

    public LiveStatsResponse pollLiveStats(
            final Long liveStreamingId,
            final String clientId,
            final Long userId
    ) {
        liveStreamingViewerManager.recordHeartbeat(liveStreamingId, clientId, userId);

        final int viewerCount = liveStreamingViewerManager.getViewerCount(liveStreamingId);
        final int likeCount = reactionReader.countBy(liveStreamingId, ReactionType.LIKE);
        return new LiveStatsResponse(viewerCount, likeCount);
    }
}
