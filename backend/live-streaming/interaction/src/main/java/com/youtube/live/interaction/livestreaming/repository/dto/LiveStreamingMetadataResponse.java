package com.youtube.live.interaction.livestreaming.repository.dto;

import java.time.Instant;

public record LiveStreamingMetadataResponse(
        Long channelId,
        String channelName,
        String channelProfileImageUrl,
        String liveStreamingTitle,
        String liveStreamingDescription,
        Instant liveStreamingStartedAt,
        Long subscriberCount
) {
}
