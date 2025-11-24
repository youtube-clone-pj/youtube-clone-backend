package com.youtube.live.interaction.livestreaming.service.dto;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;

public record LiveStreamingCreateResponse(
        Long liveStreamingId,
        String title,
        String description,
        String thumbnailUrl,
        LiveStreamingStatus status,
        Long channelId) {
}
