package com.youtube.live.interaction.livestreaming.repository.dto;

import com.youtube.live.interaction.livestreaming.domain.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long chatId;
    private String username;
    private String message;
    private ChatMessageType chatMessageType;
    private String userProfileImageUrl;
    private Instant timestamp;
}
