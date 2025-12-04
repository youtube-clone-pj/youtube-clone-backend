package com.youtube.live.interaction.livestreaming.controller.dto;

import com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse;

import java.util.List;

public record InitialChatMessagesResponse (List<ChatMessageResponse> messages) {
}
