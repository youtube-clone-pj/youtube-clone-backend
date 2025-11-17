package com.youtube.live.interaction.livestreaming.service;

import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingChatReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveStreamingChatQueryService {

    private final LiveStreamingChatReader liveStreamingChatReader;

    public List<ChatMessageResponse> getInitialMessages(final Long liveStreamingId) {
        final List<ChatMessageResponse> recentChats = liveStreamingChatReader.readRecentChats(liveStreamingId, 30);
        Collections.reverse(recentChats);
        return recentChats;
    }
}
