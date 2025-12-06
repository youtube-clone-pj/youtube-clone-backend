package com.youtube.live.interaction.websocket.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * HTTP 세션의 clientId를 WebSocket 세션 속성으로 전달하는 인터셉터
 *
 * HttpSessionHandshakeInterceptor를 상속받아 HTTP 세션의 모든 속성을 복사하고,
 * 추가로 clientId를 WebSocket 세션 속성으로 전달합니다.
 */
public class CustomHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    private static final String SESSION_CLIENT_ID = "clientId";

    @Override
    public boolean beforeHandshake(
            final ServerHttpRequest request,
            final ServerHttpResponse response,
            final WebSocketHandler wsHandler,
            final Map<String, Object> attributes
    ) throws Exception {
        // 기본 HTTP 세션 속성 복사
        final boolean result = super.beforeHandshake(request, response, wsHandler, attributes);

        // HTTP 세션에서 clientId 가져오기 (또는 세션 ID를 clientId로 사용)
        if (request instanceof ServletServerHttpRequest) {
            final HttpSession session = ((ServletServerHttpRequest) request)
                    .getServletRequest()
                    .getSession(false);

            if (session != null) {
                // clientId가 이미 세션에 있으면 사용, 없으면 세션 ID를 clientId로 사용
                String clientId = (String) session.getAttribute(SESSION_CLIENT_ID);
                if (clientId == null) {
                    clientId = session.getId();
                }
                attributes.put(SESSION_CLIENT_ID, clientId);
            }
        }

        return result;
    }
}
