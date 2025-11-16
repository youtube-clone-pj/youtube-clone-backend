package com.youtube.live.interaction.livestreaming.controller.dto;

import com.youtube.live.interaction.livestreaming.domain.ReactionType;
import lombok.Data;

@Data
public class ReactionCreateRequest {

    private final ReactionType reactionType;
}