package com.youtube.live.interaction.config;

import com.youtube.live.interaction.websocket.auth.AuthUserArgumentResolver;
import com.youtube.live.interaction.websocket.auth.CustomHandshakeInterceptor;
import com.youtube.live.interaction.websocket.auth.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket 설정 클래스
 *
 * STOMP 프로토콜을 사용하여 실시간 채팅 기능을 위한 WebSocket 통신을 설정합니다.
 * 클라이언트는 WebSocket을 통해 실시간으로 메시지를 주고받을 수 있습니다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private TaskScheduler messageBrokerTaskScheduler;

    /**
     * STOMP heartbeat를 위한 TaskScheduler를 주입받습니다.
     *
     * @Lazy 어노테이션을 사용하여 순환 참조 문제를 방지합니다.
     * Spring이 제공하는 기본 TaskScheduler를 사용하며, 필요할 때까지 초기화를 지연시킵니다.
     *
     * @see <a href="https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-simple-broker.html">Spring Framework - Simple Broker</a>
     */
    @Autowired
    public void setMessageBrokerTaskScheduler(@Lazy final TaskScheduler taskScheduler) {
        this.messageBrokerTaskScheduler = taskScheduler;
    }

    /**
     * 메시지 브로커를 설정합니다.
     *
     * SimpleBroker를 활성화하여 구독 관리 및 메시지 라우팅을 자동으로 처리합니다.
     * - "/topic" 접두사: 클라이언트가 구독할 수 있는 목적지 (예: /topic/livestreams/1/chat/messages)
     *   SimpleBroker가 자동으로 구독자를 관리하고 브로드캐스트합니다.
     * - "/queue" 접두사: 개별 사용자에게 메시지를 전송할 때 사용 (예: /user/queue/errors)
     *   @SendToUser 어노테이션과 함께 사용됩니다.
     * - "/app" 접두사: 클라이언트가 서버의 @MessageMapping 메서드로 메시지를 전송할 때 사용
     *   (예: /app/livestreams/1/chat/messages → @MessageMapping("/livestreams/{livestreamId}/chat/messages"))
     *
     * setPreservePublishOrder(true): 서버에서 클라이언트로 메시지를 발행할 때 순서를 보장합니다.
     * 같은 세션의 아웃바운드 메시지가 순서대로 전송됩니다.
     *
     * STOMP heartbeat 설정:
     * - setTaskScheduler: TaskScheduler를 설정하여 heartbeat 기능을 활성화합니다.
     * - setHeartbeatValue: heartbeat 간격을 설정합니다 (밀리초 단위).
     *   [서버→클라이언트 간격, 클라이언트→서버 간격]
     *   - 서버→클라이언트: 10초 (10000ms) - 서버가 클라이언트로 주기적으로 heartbeat를 전송
     *   - 클라이언트→서버: 10초 (10000ms) - 클라이언트로부터 heartbeat를 기대하는 간격
     *   Heartbeat는 연결 활성 상태를 확인하기 위해 전송되며, 클라이언트가 설정된 간격 내에
     *   heartbeat를 보내지 않으면 서버는 해당 연결을 비정상으로 간주하고 종료합니다.
     *
     * @see <a href="https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-simple-broker.html">Spring Framework - Simple Broker Heartbeat</a>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(Destinations.TOPIC_PREFIX, Destinations.QUEUE_PREFIX)
                .setTaskScheduler(this.messageBrokerTaskScheduler)
                .setHeartbeatValue(new long[]{10000, 10000}); // [서버→클라이언트, 클라이언트→서버] 간격 (ms)
        config.setApplicationDestinationPrefixes(Destinations.APP_PREFIX);
        config.setPreservePublishOrder(true);
    }

    /**
     * STOMP 엔드포인트를 등록합니다.
     *
     * 클라이언트가 WebSocket 서버에 연결할 수 있는 엔드포인트를 정의합니다.
     *
     * withSockJS(): SockJS 프로토콜을 활성화하여 네트워크 환경에 따라 자동으로 전송 방식을 선택합니다.
     * - WebSocket 연결을 최우선으로 시도
     * - 실패 시 HTTP 스트리밍/폴링으로 자동 fallback (프록시, 방화벽 등의 제약 우회)
     * - 클라이언트는 SockJS 프로토콜을 지원하는 클라이언트(SockJsClient, sockjs-client)를 사용해야 합니다
     *
     * CustomHandshakeInterceptor: WebSocket 연결이 시작될 때 기존 HTTP 세션의 속성들을
     * WebSocket 세션으로 복사하고, clientId를 WebSocket 세션 속성으로 전달합니다.
     *
     * @see <a href="https://docs.spring.io/spring-framework/reference/web/websocket/fallback.html">Spring Framework - SockJS Fallback</a>
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(Destinations.WS_ENDPOINT)
                .setAllowedOriginPatterns("*") //TODO Same Origin에서만 가능하도록 수정할 것
                .addInterceptors(new CustomHandshakeInterceptor())
                .withSockJS();

        // 클라이언트에서 서버로 수신된 메시지를 순서대로 처리
        registry.setPreserveReceiveOrder(true);
    }

    /**
     * 클라이언트 인바운드 채널에 인터셉터를 등록합니다.
     *
     * WebSocketAuthInterceptor를 등록하여 STOMP CONNECT 메시지에서
     * 인증 정보를 검증하고 Principal을 설정합니다.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketAuthInterceptor());
    }

    /**
     * 커스텀 ArgumentResolver를 등록합니다.
     *
     * AuthUserArgumentResolver를 등록하여 @AuthUser 어노테이션이 붙은 파라미터에
     * 인증된 사용자 정보를 주입합니다.
     */
    @Override
    public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthUserArgumentResolver());
    }

    /**
     * WebSocket 관련 경로들을 관리하는 상수 클래스
     */
    public static class Destinations {
        // Broker prefixes
        public static final String TOPIC_PREFIX = "/topic";
        public static final String QUEUE_PREFIX = "/queue";
        public static final String APP_PREFIX = "/app";

        // WebSocket endpoint
        public static final String WS_ENDPOINT = "/ws";

        // Topic destinations
        public static final String CHAT_LIVESTREAM_MESSAGES_TOPIC = "/topic/livestreams/{livestreamId}/chat/messages";
        public static final String CHAT_LIVESTREAM_VIEWER_COUNT_TOPIC = "/topic/livestreams/{livestreamId}/viewer-count";

        public static String getChatLivestreamMessagesTopic(Long livestreamId) {
            return CHAT_LIVESTREAM_MESSAGES_TOPIC.replace("{livestreamId}", String.valueOf(livestreamId));
        }

        public static String getChatLivestreamViewerCountTopic(Long livestreamId) {
            return CHAT_LIVESTREAM_VIEWER_COUNT_TOPIC.replace("{livestreamId}", String.valueOf(livestreamId));
        }
    }
}
