package com.youtube.live.interaction.livestreaming.service.dto;

import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.domain.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class LiveStreamingChatInfo {

    private String username;
    private String message;
    private ChatMessageType chatMessageType;
    private String userProfileImageUrl;
    private Instant timestamp;

    public static LiveStreamingChatInfo of(
            final User user,
            final String message,
            final ChatMessageType chatMessageType,
            final Instant timestamp)
    {
        return new LiveStreamingChatInfo(user.getUsername(), message, chatMessageType, user.getProfileImageUrl(), timestamp);
    }
}
