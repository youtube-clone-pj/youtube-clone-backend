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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LiveStreamingReactionService {

    private final ReactionReader reactionReader;
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

        final ReactionType resultType = processReactionToggle(liveStreaming, user, requestType);
        publishLikeCountEventIfNeeded(liveStreamingId, resultType);

        return new ReactionToggleResult(resultType);
    }

    private void publishLikeCountEventIfNeeded(final Long liveStreamingId, final ReactionType resultType) {
        if (resultType == ReactionType.LIKE) {
            eventPublisher.publishEvent(new ReactionEvent(liveStreamingId));
        }
    }

    private ReactionType processReactionToggle(
            final LiveStreaming liveStreaming, final User user, final ReactionType requestType
    ) {
        final Optional<LiveStreamingReaction> existingReaction = reactionReader.readBy(liveStreaming.getId(), user.getId());
        if (existingReaction.isEmpty()) {
            createReaction(liveStreaming, user, requestType);
            return requestType;
        }

        final LiveStreamingReaction reaction = existingReaction.get();
        if (reaction.isSameType(requestType)) {
            reactionWriter.remove(reaction);
            return null;
        }
        reaction.changeType(requestType);

        return requestType;
    }

    private void createReaction(final LiveStreaming liveStreaming, final User user, final ReactionType type) {
        reactionWriter.write(LiveStreamingReaction.builder()
            .liveStreaming(liveStreaming)
            .user(user)
            .type(type)
            .build()
        );
    }
}
