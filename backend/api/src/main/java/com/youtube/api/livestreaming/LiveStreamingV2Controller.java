package com.youtube.api.livestreaming;

import com.youtube.common.exception.AuthErrorCode;
import com.youtube.common.exception.BaseException;
import com.youtube.live.interaction.exception.LiveStreamingErrorCode;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageRequest;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingChatService;
import com.youtube.live.interaction.livestreaming.service.dto.ChatsResponse;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStatsResponse;
import com.youtube.live.interaction.livestreaming.repository.dto.LiveStreamingMetadataResponse;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingChatQueryService;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingQueryService;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingChatInfo;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/livestreams")
public class LiveStreamingV2Controller {

    private final LiveStreamingQueryService liveStreamingQueryService;
    private final LiveStreamingChatQueryService liveStreamingChatQueryService;
    private final LiveStreamingChatService liveStreamingChatService;
    private static final String SESSION_USER_ID = "userId";
    private static final String SESSION_CLIENT_ID = "clientId";

    @GetMapping("/{liveStreamingId}/metadata")
    public ResponseEntity<LiveStreamingMetadataResponse> getMetadata(
            @PathVariable final Long liveStreamingId
    ) {
        final LiveStreamingMetadataResponse response = liveStreamingQueryService.getMetadata(liveStreamingId);
        return ResponseEntity.ok(response);
    }

    /**
     * V2 (Polling): 실시간 통계 조회 + Heartbeat 업데이트
     *
     * Side Effect: 이 GET 요청은 사용자의 heartbeat를 기록하여 실시간 시청자
     수에 영향을 줍니다.
     * Polling 효율성을 위해 조회와 heartbeat를 결합했습니다.
     */
    @GetMapping("/{liveStreamingId}/live-stats")
    public ResponseEntity<LiveStatsResponse> getLiveStats(
            @PathVariable final Long liveStreamingId,
            final HttpSession session
    ) {
        final String clientId = getOrCreateClientId(session);
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);

        final LiveStatsResponse response = liveStreamingQueryService.pollLiveStats(
                liveStreamingId,
                clientId,
                userId
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{liveStreamingId}/chats")
    public ResponseEntity<ChatsResponse> getChats(
            @PathVariable final Long liveStreamingId,
            @RequestParam(required = false) final Long lastChatId
    ) {
        if (lastChatId != null && lastChatId <= 0) {
            throw new BaseException(LiveStreamingErrorCode.INVALID_LAST_CHAT_ID);
        }

        final ChatsResponse response;
        if (lastChatId == null) {
            response = liveStreamingChatQueryService.getInitialChats(liveStreamingId);
        } else {
            response = liveStreamingChatQueryService.getNewChats(liveStreamingId, lastChatId);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{liveStreamingId}/chats")
    public ResponseEntity<LiveStreamingChatInfo> sendChat(
            @PathVariable final Long liveStreamingId,
            @RequestBody final ChatMessageRequest request,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        final LiveStreamingChatInfo chatInfo = liveStreamingChatService.sendMessage(
                liveStreamingId,
                userId,
                request.getMessage(),
                request.getChatMessageType(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(chatInfo);
    }

    /**
     * 세션에서 clientId를 가져오거나 새로 생성
     *
     * clientId는 비로그인 사용자를 식별하기 위한 서버 생성 고유 ID입니다.
     * 세션에 한 번 생성되면 세션이 유지되는 동안 재사용됩니다.
     */
    private String getOrCreateClientId(final HttpSession session) {
        String clientId = (String) session.getAttribute(SESSION_CLIENT_ID);
        if (clientId == null) {
            clientId = UUID.randomUUID().toString();
            session.setAttribute(SESSION_CLIENT_ID, clientId);
        }
        return clientId;
    }
}
