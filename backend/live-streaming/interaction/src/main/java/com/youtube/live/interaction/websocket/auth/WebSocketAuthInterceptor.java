package com.youtube.live.interaction.websocket.auth;

import com.youtube.common.exception.AuthErrorCode;
import com.youtube.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * WebSocket 메시지의 인증을 처리하는 인터셉터
 * <p>
 * STOMP CONNECT 메시지가 처리될 때 세션 속성의 인증 정보를 확인하여 적절한 Principal을 설정합니다.
 */
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            final Long userId = (Long) accessor.getSessionAttributes().get("userId");
            final String username = (String) accessor.getSessionAttributes().get("username");

            if (userId != null && username != null) {
                accessor.setUser(new AuthenticatedPrincipal(userId, username));
            } else if( userId == null && username == null){
                accessor.setUser(new UnauthenticatedPrincipal());
            } else {
                log.error("WebSocket 연결 실패 - sessionId: {}, userId: {}, username 존재 여부: {}",
                        accessor.getSessionId(), userId, username != null);
                throw new BaseException(AuthErrorCode.INVALID_CREDENTIALS);
            }
        }

        return message;
    }
}
