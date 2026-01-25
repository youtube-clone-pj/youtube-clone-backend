package com.youtube.live.interaction.livestreaming.service;

import com.youtube.live.interaction.livestreaming.domain.*;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingChatInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LiveStreamingChatService {

    private final LiveStreamingReader liveStreamingReader;
    private final LiveStreamingChatWriter liveStreamingChatWriter;

    @Transactional
    public LiveStreamingChatInfo sendMessage(
            final Long liveStreamingId,
            final Long userId,
            final String username,
            final String profileImageUrl,
            final String message,
            final ChatMessageType messageType,
            final Instant now
    ) {
        final LiveStreamingStatus status = liveStreamingReader.readCachedStatusBy(liveStreamingId);
        final LiveStreaming liveStreaming = liveStreamingReader.getReferenceBy(liveStreamingId);

        liveStreamingChatWriter.write(
                liveStreaming,
                status,
                userId,
                username,
                profileImageUrl,
                message,
                messageType
        );

        return LiveStreamingChatInfo.of(userId, username, profileImageUrl, message, messageType, now);
    }
}
