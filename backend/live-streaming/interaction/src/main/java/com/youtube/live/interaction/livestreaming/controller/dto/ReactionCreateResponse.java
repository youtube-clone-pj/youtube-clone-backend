package com.youtube.live.interaction.livestreaming.controller.dto;

import lombok.Data;

@Data
public class ReactionCreateResponse {

    private final int likeCount;
    private final boolean isLiked;
    private final boolean isDisliked;
}
