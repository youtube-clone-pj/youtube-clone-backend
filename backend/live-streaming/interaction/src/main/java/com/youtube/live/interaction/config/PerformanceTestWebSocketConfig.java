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
 * 성능 테스트 전용 WebSocket 설정
 *
 * ⚠️ WARNING: 프로덕션용과 동일한 설정을 유지하되, SockJS만 제거
 *
 * 차이점:
 * - Raw WebSocket 사용 (SockJS 미사용)
 * - 엔드포인트: /ws-direct
 *
 * 동일한 설정:
 * - SimpleBroker, TaskScheduler, Heartbeat
 * - CustomHandshakeInterceptor
 * - WebSocketAuthInterceptor
 * - AuthUserArgumentResolver
 * - 메시지 순서 보장
 *
 * 목적: Gatling 성능 테스트를 위한 Raw WebSocket 엔드포인트 제공
 */
//TODO 프로파일 설정
@Configuration
@EnableWebSocketMessageBroker
public class PerformanceTestWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private TaskScheduler messageBrokerTaskScheduler;

    /**
     * STOMP heartbeat를 위한 TaskScheduler를 주입받습니다.
     *
     * @Lazy 어노테이션을 사용하여 순환 참조 문제를 방지합니다.
     */
    @Autowired
    public void setMessageBrokerTaskScheduler(@Lazy final TaskScheduler taskScheduler) {
        this.messageBrokerTaskScheduler = taskScheduler;
    }

    /**
     * 메시지 브로커를 설정합니다.
     *
     * SimpleBroker를 활성화하여 구독 관리 및 메시지 라우팅을 자동으로 처리합니다.
     * - "/topic" 접두사: 클라이언트가 구독할 수 있는 목적지
     * - "/queue" 접두사: 개별 사용자에게 메시지를 전송할 때 사용
     * - "/app" 접두사: 클라이언트가 서버의 @MessageMapping 메서드로 메시지를 전송할 때 사용
     *
     * setPreservePublishOrder(true): 서버에서 클라이언트로 메시지를 발행할 때 순서를 보장합니다.
     *
     * STOMP heartbeat 설정:
     * - setTaskScheduler: TaskScheduler를 설정하여 heartbeat 기능을 활성화
     * - setHeartbeatValue: [서버→클라이언트 10초, 클라이언트→서버 10초]
     */
    @Override
    public void configureMessageBroker(final MessageBrokerRegistry config) {
        config.enableSimpleBroker(WebSocketConfig.Destinations.TOPIC_PREFIX, WebSocketConfig.Destinations.QUEUE_PREFIX)
                .setTaskScheduler(this.messageBrokerTaskScheduler)
                .setHeartbeatValue(new long[]{10000, 10000});
        config.setApplicationDestinationPrefixes(WebSocketConfig.Destinations.APP_PREFIX);
        config.setPreservePublishOrder(true);
    }

    /**
     * STOMP 엔드포인트를 등록합니다.
     *
     * ⚠️ 프로덕션과의 차이점: SockJS 미사용, Raw WebSocket만 사용
     *
     * CustomHandshakeInterceptor: WebSocket 연결이 시작될 때 기존 HTTP 세션의 속성들을
     * WebSocket 세션으로 복사하고, clientId를 WebSocket 세션 속성으로 전달합니다.
     */
    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        // Raw WebSocket (SockJS 없음)
        registry.addEndpoint("/ws-direct")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new CustomHandshakeInterceptor());

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
    public void configureClientInboundChannel(final ChannelRegistration registration) {
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
}
