package com.youtube.live.interaction.livestreaming.service.dto;


public record LiveStreamingCreateRequest(
        String title,
        String description,
        String thumbnailUrl) {
}
