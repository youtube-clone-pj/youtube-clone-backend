package com.youtube.live.interaction.livestreaming.service;

import com.youtube.live.interaction.livestreaming.service.dto.LikeStatusResponse;
import com.youtube.live.interaction.livestreaming.domain.ReactionReader;
import com.youtube.live.interaction.livestreaming.domain.ReactionType;
import com.youtube.live.interaction.livestreaming.service.dto.ReactionToggleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveStreamingReactionQueryService {

    private final ReactionReader reactionReader;

    public int getLikeCount(final Long liveStreamingId) {
        return reactionReader.countBy(liveStreamingId, ReactionType.LIKE);
    }

    public LikeStatusResponse getLikeStatus(final Long liveStreamingId, final Long userId) {
        final int likeCount = reactionReader.countBy(liveStreamingId, ReactionType.LIKE);
        final ReactionToggleResult userReaction = reactionReader.readUserReaction(liveStreamingId, userId);

        return new LikeStatusResponse(
                likeCount,
                userReaction.isLiked(),
                userReaction.isDisliked()
        );
    }
}
