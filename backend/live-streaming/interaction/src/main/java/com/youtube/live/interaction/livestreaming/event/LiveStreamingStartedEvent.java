package com.youtube.live.interaction.livestreaming.event;

public record LiveStreamingStartedEvent(
        Long liveStreamingId,
        Long channelId
) {
}
