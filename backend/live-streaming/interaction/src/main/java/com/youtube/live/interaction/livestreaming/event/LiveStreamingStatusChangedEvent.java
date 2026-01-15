package com.youtube.live.interaction.livestreaming.event;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;

public record LiveStreamingStatusChangedEvent(
        Long liveStreamingId,
        LiveStreamingStatus status
) {
}
