package com.youtube.live.interaction.livestreaming.controller;

import com.youtube.live.interaction.websocket.auth.AuthUser;
import com.youtube.live.interaction.websocket.auth.LoginUser;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageRequest;
import com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.controller.dto.ErrorResponse;
import com.youtube.live.interaction.livestreaming.controller.dto.InitialChatMessagesResponse;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingChatQueryService;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingChatService;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingChatInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final LiveStreamingChatService liveStreamingChatService;
    private final LiveStreamingChatQueryService liveStreamingChatQueryService;


    /**
     * 클라이언트가 구독 시 초기 채팅 메시지를 전송합니다.
     *
     * 주의: @SubscribeMapping에서 List 타입을 직접 반환하면 Spring STOMP가 제대로 직렬화하지 못합니다.
     * 따라서 List를 DTO로 래핑하여 반환해야 합니다.
     */
    @SubscribeMapping("/livestreams/{livestreamId}/chat/messages")
    public InitialChatMessagesResponse loadInitialMessages(@DestinationVariable final Long livestreamId) {
        final List<ChatMessageResponse> messages = liveStreamingChatQueryService.getInitialMessages(livestreamId);
        log.debug("@SubscribeMapping 호출됨 - livestreamId: {}, 반환 메시지 개수: {}", livestreamId, messages.size());

        return new InitialChatMessagesResponse(messages);
    }

    @MessageMapping("/livestreams/{livestreamId}/chat/messages")
    @SendTo("/topic/livestreams/{livestreamId}/chat/messages")
    public ChatMessageResponse sendMessage(@DestinationVariable final Long livestreamId,
                                           @Payload final ChatMessageRequest chatMessageRequest,
                                           @AuthUser LoginUser loginUser
    ) {
        final Long userId = loginUser.getUserId();
        final String username = loginUser.getUsername();
        final String profileImageUrl = loginUser.getProfileImageUrl();
        final Instant now = Instant.now();

        final LiveStreamingChatInfo savedChatInfo = liveStreamingChatService.sendMessage(
                livestreamId,
                userId,
                username,
                profileImageUrl,
                chatMessageRequest.getMessage(),
                chatMessageRequest.getChatMessageType(),
                now
        );

        return new ChatMessageResponse(
                null,
                username,
                chatMessageRequest.getMessage(),
                chatMessageRequest.getChatMessageType(),
                profileImageUrl,
                now
        );
    }

    @MessageExceptionHandler
    @SendToUser(value = "/queue/errors", broadcast = false)
    public ErrorResponse handleException(final Exception exception) {
        log.warn("채팅 메시지 처리 실패 - error: {}", exception.getMessage());
        return new ErrorResponse(exception.getMessage());
    }
}
