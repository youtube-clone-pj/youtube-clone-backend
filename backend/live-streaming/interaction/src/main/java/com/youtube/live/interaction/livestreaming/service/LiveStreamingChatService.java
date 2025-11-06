package com.youtube.live.interaction.livestreaming.service;

import com.youtube.core.user.domain.User;
import com.youtube.core.user.domain.UserReader;
import com.youtube.live.interaction.livestreaming.domain.*;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingChatInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiveStreamingChatService {

    private final LiveStreamingReader liveStreamingReader;
    private final UserReader userReader;
    private final LiveStreamingChatWriter liveStreamingChatWriter;

    @Transactional
    public LiveStreamingChatInfo save(final Long liveStreamingId, final Long userId, final String message,
                                      final ChatMessageType messageType) {
        final LiveStreaming liveStreaming = liveStreamingReader.readBy(liveStreamingId);
        final User user = userReader.readBy(userId);

        liveStreamingChatWriter.write(liveStreaming, user, message, messageType);

        return LiveStreamingChatInfo.of(user, message, messageType);
    }
}
