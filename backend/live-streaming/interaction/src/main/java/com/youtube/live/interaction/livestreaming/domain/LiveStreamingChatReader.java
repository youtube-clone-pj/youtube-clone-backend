package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.repository.LiveStreamingChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LiveStreamingChatReader {

    private static final int DEFAULT_CHAT_SIZE = 50;

    private final LiveStreamingChatRepository liveStreamingChatRepository;

    public List<ChatMessageResponse> readRecentChats(
            final Long liveStreamingId,
            final int pageSize) {
        return liveStreamingChatRepository.findByLiveStreamingIdOrderByCreatedDateDesc(
                liveStreamingId,
                cursorPageable(pageSize)
        );
    }

    public List<ChatMessageResponse> readNewChatsAfter(
            final Long liveStreamingId,
            final Long lastChatId) {
        return liveStreamingChatRepository.findNewChatsAfter(
                liveStreamingId,
                lastChatId,
                cursorPageable(DEFAULT_CHAT_SIZE)
        );
    }

    /**
     * 커서 기반 페이지네이션을 위한 Pageable 생성
     * offset은 항상 0으로 고정
     */
    private static Pageable cursorPageable(final int size) {
        return PageRequest.of(0, size);
    }
}
