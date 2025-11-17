package com.youtube.live.interaction.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestStompClientFactory {

    /**
     * STOMP over SockJS 클라이언트를 생성합니다.
     *
     * SockJS는 네트워크/전송 계층에서 다음 순서로 연결을 시도합니다:
     * 1. WebSocketTransport: 네이티브 WebSocket 연결 (최우선)
     * 2. RestTemplateXhrTransport: HTTP 기반 폴백
     *    - xhr-streaming: 서버→클라이언트 메시지를 위한 장기 실행 요청
     *    - xhr-polling: streaming 실패 시, 각 메시지마다 요청 종료 후 재연결
     *
     * 서버가 withSockJS()로 설정된 경우, 클라이언트는 SockJS 프로토콜 핸드셰이크(GET /info 등)를
     * 수행하는 SockJsClient를 사용해야 합니다.
     *
     * @return 설정된 WebSocketStompClient 인스턴스
     * @see <a href="https://docs.spring.io/spring-framework/reference/web/websocket/fallback.html">Spring Framework - SockJS Fallback</a>
     */
    static WebSocketStompClient create() {
        final List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient()),
                new RestTemplateXhrTransport(new RestTemplate())
        );

        final WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(transports));
        configureMessageConverter(stompClient);

        return stompClient;
    }

    private static void configureMessageConverter(final WebSocketStompClient stompClient) {
        // ObjectMapper에 JavaTimeModule 등록하여 Instant 역직렬화 가능하도록 설정
        final MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.getObjectMapper().findAndRegisterModules();
        stompClient.setMessageConverter(messageConverter);
    }
}
