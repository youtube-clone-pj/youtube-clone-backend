package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.repository.LiveStreamingChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveStreamingChatWriter {

    private final LiveStreamingChatRepository liveStreamingChatRepository;

    public Long write(final LiveStreaming liveStreaming, final User user, final String message,
                      final ChatMessageType messageType) {
        final LiveStreamingChat chat = LiveStreamingChat.builder()
                .liveStreaming(liveStreaming)
                .userId(user.getId())
                .username(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .message(message)
                .messageType(messageType)
                .build();

        return liveStreamingChatRepository.save(chat).getId();
    }

    public Long write(
            final LiveStreaming liveStreaming,
            final LiveStreamingStatus liveStreamingStatus,
            final Long userId,
            final String username,
            final String profileImageUrl,
            final String message,
            final ChatMessageType messageType
    ) {
        final LiveStreamingChat chat = LiveStreamingChat.builder()
                .liveStreaming(liveStreaming)
                .liveStreamingStatus(liveStreamingStatus)
                .userId(userId)
                .username(username)
                .profileImageUrl(profileImageUrl)
                .message(message)
                .messageType(messageType)
                .build();

        return liveStreamingChatRepository.save(chat).getId();
    }
}
