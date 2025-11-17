package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.repository.LiveStreamingChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LiveStreamingChatReader {

    private final LiveStreamingChatRepository liveStreamingChatRepository;

    public List<ChatMessageResponse> readRecentChats(
            final Long liveStreamingId,
            final int pageSize) {
        return liveStreamingChatRepository.findByLiveStreamingIdOrderByCreatedDateDesc(
                liveStreamingId,
                PageRequest.of(0, pageSize)
        );
    }
}
