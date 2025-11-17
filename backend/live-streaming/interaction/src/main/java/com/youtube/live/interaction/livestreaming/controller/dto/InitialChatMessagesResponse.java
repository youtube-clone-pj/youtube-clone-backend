package com.youtube.live.interaction.livestreaming.controller.dto;

import java.util.List;

public record InitialChatMessagesResponse (List<ChatMessageResponse> messages) {
}
