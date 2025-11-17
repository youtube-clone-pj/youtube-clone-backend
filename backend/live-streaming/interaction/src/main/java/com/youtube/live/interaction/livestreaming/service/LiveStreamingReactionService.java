package com.youtube.live.interaction.livestreaming.service;

import com.youtube.core.user.domain.User;
import com.youtube.core.user.domain.UserReader;
import com.youtube.live.interaction.livestreaming.domain.*;
import com.youtube.live.interaction.livestreaming.service.dto.ReactionToggleResult;
import com.youtube.live.interaction.websocket.event.dto.ReactionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiveStreamingReactionService {

    private final ReactionWriter reactionWriter;
    private final LiveStreamingReader liveStreamingReader;
    private final UserReader userReader;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReactionToggleResult toggleReaction(
        final Long liveStreamingId, final Long userId, final ReactionType requestType
    ) {
        final LiveStreaming liveStreaming = liveStreamingReader.readBy(liveStreamingId);
        final User user = userReader.readBy(userId);

        final ReactionType resultType = reactionWriter.processToggle(liveStreaming, user, requestType);
        publishLikeCountEventIfNeeded(liveStreamingId, resultType);

        return new ReactionToggleResult(resultType);
    }

    private void publishLikeCountEventIfNeeded(final Long liveStreamingId, final ReactionType resultType) {
        if (resultType == ReactionType.LIKE) {
            eventPublisher.publishEvent(new ReactionEvent(liveStreamingId));
        }
    }
}
