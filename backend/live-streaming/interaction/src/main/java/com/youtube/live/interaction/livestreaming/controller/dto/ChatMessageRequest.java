package com.youtube.live.interaction.livestreaming.controller.dto;

import com.youtube.live.interaction.livestreaming.domain.ChatMessageType;
import lombok.Data;

@Data
public class ChatMessageRequest {

    private String message;
    private ChatMessageType chatMessageType;
}
