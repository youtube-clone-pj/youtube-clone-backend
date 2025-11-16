package com.youtube.api.livestreaming;

import com.youtube.live.interaction.livestreaming.controller.dto.ReactionCreateRequest;
import com.youtube.live.interaction.livestreaming.controller.dto.ReactionCreateResponse;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingReactionService;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingReactionQueryService;
import com.youtube.live.interaction.livestreaming.service.dto.ReactionToggleResult;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/livestreams")
public class LiveStreamingController {

    private final LiveStreamingReactionService liveStreamingReactionService;
    private final LiveStreamingReactionQueryService liveStreamingReactionQueryService;
    private static final String SESSION_USER_ID = "userId";

    @PostMapping("/{liveStreamingId}/likes")
    public ResponseEntity<ReactionCreateResponse> toggleLike(
        @PathVariable final Long liveStreamingId,
        @RequestBody final ReactionCreateRequest request,
        final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다");
        }

        final ReactionToggleResult toggleResult = liveStreamingReactionService.toggleReaction(
            liveStreamingId,
            userId,
            request.getReactionType()
        );

        final int likeCount = liveStreamingReactionQueryService.getLikeCount(liveStreamingId);

        final ReactionCreateResponse response = new ReactionCreateResponse(
            likeCount,
            toggleResult.isLiked(),
            toggleResult.isDisliked()
        );

        return ResponseEntity.ok(response);
    }
}
