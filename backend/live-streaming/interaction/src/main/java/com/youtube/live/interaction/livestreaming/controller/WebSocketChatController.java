package com.youtube.live.interaction.livestreaming.controller;

import com.youtube.live.interaction.websocket.auth.AuthUser;
import com.youtube.live.interaction.websocket.auth.LoginUser;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageRequest;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.controller.dto.ErrorResponse;
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
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final LiveStreamingChatService liveStreamingChatService;

    @MessageMapping("/livestreams/{livestreamId}/chat/messages")
    @SendTo("/topic/livestreams/{livestreamId}/chat/messages")
    public ChatMessageResponse sendMessage(@DestinationVariable final Long livestreamId,
                                           @Payload final ChatMessageRequest chatMessageRequest,
                                           @AuthUser LoginUser loginUser
    ) {
        final Long userId = loginUser.getUserId();
        final String username = loginUser.getUsername();

        final LiveStreamingChatInfo savedChatInfo = liveStreamingChatService.sendMessage(
                livestreamId,
                userId,
                chatMessageRequest.getMessage(),
                chatMessageRequest.getChatMessageType()
        );

        return new ChatMessageResponse(
                username,
                chatMessageRequest.getMessage(),
                chatMessageRequest.getChatMessageType(),
                savedChatInfo.getUserProfileImageUrl(),
                LocalDateTime.now()
        );
    }

    @MessageExceptionHandler
    @SendToUser(value = "/queue/errors", broadcast = false)
    public ErrorResponse handleException(final Exception exception) {
        log.warn("채팅 메시지 처리 실패 - error: {}", exception.getMessage());
        return new ErrorResponse(exception.getMessage());
    }
}
