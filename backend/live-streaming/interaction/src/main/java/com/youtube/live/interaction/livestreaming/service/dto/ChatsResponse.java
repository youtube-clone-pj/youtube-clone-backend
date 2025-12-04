package com.youtube.live.interaction.livestreaming.service.dto;

import com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse;

import java.util.List;

public record ChatsResponse(
        List<ChatMessageResponse> chats,
        Long lastChatId
) {
}