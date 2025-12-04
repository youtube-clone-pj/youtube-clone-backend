package com.youtube.live.interaction.livestreaming.service;

import com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.service.dto.ChatsResponse;
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

    public ChatsResponse getInitialChats(final Long liveStreamingId) {
        final List<ChatMessageResponse> recentChats = getInitialMessages(liveStreamingId);
        return new ChatsResponse(recentChats, extractLastChatId(recentChats));
    }

    public ChatsResponse getNewChats(final Long liveStreamingId, final Long lastChatId) {
        final List<ChatMessageResponse> newChats = liveStreamingChatReader.readNewChatsAfter(liveStreamingId, lastChatId);
        final Long latestChatId = newChats.isEmpty() ? lastChatId : extractLastChatId(newChats);
        return new ChatsResponse(newChats, latestChatId);
    }

    private Long extractLastChatId(final List<ChatMessageResponse> chats) {
        return chats.isEmpty() ? null : chats.get(chats.size() - 1).getChatId();
    }
}
