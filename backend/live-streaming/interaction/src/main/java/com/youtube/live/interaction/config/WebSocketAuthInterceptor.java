package com.youtube.live.interaction.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * WebSocket 메시지의 인증을 처리하는 인터셉터
 *
 * STOMP CONNECT 메시지가 처리될 때 세션의 인증 정보를 검증하고
 * Principal 객체를 설정하여 컨트롤러에서 사용할 수 있도록 합니다.
 */
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            final Long userId = (Long) accessor.getSessionAttributes().get("userId");
            final String username = (String) accessor.getSessionAttributes().get("username");

            if (userId == null || username == null) {
                log.warn("WebSocket 연결 인증 실패 - 세션 정보 없음");
                throw new IllegalStateException("인증되지 않은 사용자입니다");
            }

            // Principal 설정으로 컨트롤러에서 사용 가능하도록 함
            accessor.setUser(new StompPrincipal(userId, username));
        }

        return message;
    }
}
