package com.youtube.live.interaction.livestreaming.service.dto;

import com.youtube.live.interaction.livestreaming.domain.ReactionType;

public record ReactionToggleResult(ReactionType reactionType) {

    public boolean isLiked() {
        return reactionType == ReactionType.LIKE;
    }

    public boolean isDisliked() {
        return reactionType == ReactionType.DISLIKE;
    }
}
