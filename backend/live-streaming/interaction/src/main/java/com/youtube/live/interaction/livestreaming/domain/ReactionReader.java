package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.live.interaction.livestreaming.repository.LiveStreamingReactionRepository;
import com.youtube.live.interaction.livestreaming.service.dto.ReactionToggleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReactionReader {

    private final LiveStreamingReactionRepository liveStreamingReactionRepository;

    public Optional<LiveStreamingReaction> readBy(final Long liveStreamingId, final Long userId) {
        return liveStreamingReactionRepository.findByLiveStreamingIdAndUserId(liveStreamingId, userId);
    }

    public Optional<LiveStreamingReaction> readDeletedBy(final Long liveStreamingId, final Long userId) {
        return liveStreamingReactionRepository.findDeletedByLiveStreamingIdAndUserId(liveStreamingId, userId);
    }

    public int countBy(final Long liveStreamingId, final ReactionType type) {
        return (int) liveStreamingReactionRepository.countByLiveStreamingIdAndType(liveStreamingId, type);
    }

    public ReactionToggleResult readUserReaction(final Long liveStreamingId, final Long userId) {
        if (userId == null) {
            return new ReactionToggleResult(null);
        }

        return readBy(liveStreamingId, userId)
                .map(reaction -> new ReactionToggleResult(reaction.getType()))
                .orElse(new ReactionToggleResult(null));
    }
}
