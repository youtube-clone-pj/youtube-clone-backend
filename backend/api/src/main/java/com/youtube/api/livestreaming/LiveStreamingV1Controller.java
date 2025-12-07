package com.youtube.api.livestreaming;

import com.youtube.common.exception.AuthErrorCode;
import com.youtube.common.exception.BaseException;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateRequest;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateResponse;
import com.youtube.live.interaction.livestreaming.service.dto.LikeStatusResponse;
import com.youtube.live.interaction.livestreaming.controller.dto.ReactionCreateRequest;
import com.youtube.live.interaction.livestreaming.controller.dto.ReactionCreateResponse;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingReactionService;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingReactionQueryService;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingService;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingQueryService;
import com.youtube.live.interaction.livestreaming.repository.dto.LiveStreamingMetadataResponse;
import com.youtube.live.interaction.livestreaming.service.dto.ReactionToggleResult;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/livestreams")
public class LiveStreamingV1Controller {

    private final LiveStreamingReactionService liveStreamingReactionService;
    private final LiveStreamingReactionQueryService liveStreamingReactionQueryService;
    private final LiveStreamingService liveStreamingService;
    private final LiveStreamingQueryService liveStreamingQueryService;
    private static final String SESSION_USER_ID = "userId";

    @PostMapping("/{liveStreamingId}/likes")
    public ResponseEntity<ReactionCreateResponse> toggleLike(
            @PathVariable final Long liveStreamingId,
            @RequestBody final ReactionCreateRequest request,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
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

    @GetMapping("/{liveStreamingId}/likes")
    public ResponseEntity<LikeStatusResponse> getLikeStatus(
            @PathVariable final Long liveStreamingId,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        final LikeStatusResponse response = liveStreamingReactionQueryService.getLikeStatus(liveStreamingId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<LiveStreamingCreateResponse> startLiveStreaming(
            @RequestBody final LiveStreamingCreateRequest request,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        final LiveStreamingCreateResponse response = liveStreamingService.startLiveStreamingV1(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{liveStreamingId}/metadata")
    public ResponseEntity<LiveStreamingMetadataResponse> getMetadata(
            @PathVariable final Long liveStreamingId
    ) {
        final LiveStreamingMetadataResponse response = liveStreamingQueryService.getMetadata(liveStreamingId);
        return ResponseEntity.ok(response);
    }
}
