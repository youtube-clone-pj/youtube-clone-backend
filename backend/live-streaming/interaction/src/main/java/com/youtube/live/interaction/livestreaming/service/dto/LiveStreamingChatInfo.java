package com.youtube.live.interaction.livestreaming.service.dto;

import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.domain.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiveStreamingChatInfo {

    private String username;
    private String message;
    private ChatMessageType chatMessageType;
    private String userProfileImageUrl;

    public static LiveStreamingChatInfo of(
            final User user,
            final String message,
            final ChatMessageType chatMessageType)
    {
        return new LiveStreamingChatInfo(user.getUsername(), message, chatMessageType, user.getProfileImageUrl());
    }
}
