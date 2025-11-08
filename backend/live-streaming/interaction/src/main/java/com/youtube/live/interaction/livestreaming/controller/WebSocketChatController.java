package com.youtube.live.interaction.livestreaming.controller;

import com.youtube.live.interaction.config.WebSocketConfig;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageRequest;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.service.LiveStreamingChatService;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingChatInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveStreamingChatService liveStreamingChatService;

    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void sendMessage(@DestinationVariable final Long roomId, @Payload final ChatMessageRequest chatMessageRequest,
                            final SimpMessageHeaderAccessor headerAccessor) {
        final Long sessionUserId = (Long) headerAccessor.getSessionAttributes().get("userId");
        final String sessionUsername = (String) headerAccessor.getSessionAttributes().get("username");

        if (sessionUserId == null || sessionUsername == null) {
            log.warn("채팅 메시지 전송 실패 - 세션 정보 없음, roomId: {}", roomId);
            return;
        }

        final LiveStreamingChatInfo savedChatInfo = liveStreamingChatService.sendMessage(
                roomId,
                sessionUserId,
                chatMessageRequest.getMessage(),
                chatMessageRequest.getChatMessageType()
        );

        final ChatMessageResponse response = new ChatMessageResponse(
                sessionUsername,
                chatMessageRequest.getMessage(),
                chatMessageRequest.getChatMessageType(),
                savedChatInfo.getUserProfileImageUrl(),
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend(WebSocketConfig.Destinations.getRoomTopic(roomId), response);
    }

}
