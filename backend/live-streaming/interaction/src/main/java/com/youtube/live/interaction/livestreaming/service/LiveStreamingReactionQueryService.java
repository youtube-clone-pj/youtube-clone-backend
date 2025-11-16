package com.youtube.live.interaction.livestreaming.service;

import com.youtube.live.interaction.livestreaming.domain.ReactionReader;
import com.youtube.live.interaction.livestreaming.domain.ReactionType;
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
}
