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
                .user(user)
                .message(message)
                .messageType(messageType)
                .build();

        return liveStreamingChatRepository.save(chat).getId();
    }
}
