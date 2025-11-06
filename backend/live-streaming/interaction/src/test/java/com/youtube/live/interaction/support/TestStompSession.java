package com.youtube.live.interaction.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * STOMP 테스트를 위한 세션 래퍼 클래스
 * StompSession을 래핑하여 STOMP 관련 작업을 간편하게 수행할 수 있도록 합니다.
 *
 * @param <T> 수신할 메시지의 타입
 */
@Slf4j
public class TestStompSession<T> {

    private final StompSession session;
    private final WebSocketStompClient stompClient;
    private final Map<String, TestStompFrameHandler<T>> subscriptions = new HashMap<>();

    private TestStompSession(final StompSession session, final WebSocketStompClient stompClient) {
        this.session = session;
        this.stompClient = stompClient;
    }

    /**
     * WebSocket 연결을 생성합니다.
     * HttpSessionHandshakeInterceptor를 통해 HTTP 세션이 WebSocket 세션으로 복사됩니다.
     * 사용자 인증 정보는 HTTP 세션에서 자동으로 전달되므로, 연결 전에 TestAuthSupport.login()을 호출하여
     * JSESSIONID를 받아서 이 메서드에 전달해야 합니다.
     *
     * @param wsUrl WebSocket 엔드포인트 URL
     * @param jsessionId HTTP 세션 ID (JSESSIONID 쿠키 값). null인 경우 쿠키 없이 연결
     * @param <T> 수신할 메시지의 타입
     * @return 연결된 TestStompSession 인스턴스
     */
    public static <T> TestStompSession<T> connect(final String wsUrl, final String jsessionId)
            throws ExecutionException, InterruptedException, TimeoutException {
        final WebSocketStompClient stompClient = TestStompClientFactory.create();
        final StompSession session = connectToWebSocket(stompClient, wsUrl, jsessionId);

        return new TestStompSession<>(session, stompClient);
    }

    /**
     * WebSocket 연결을 생성합니다.
     * HttpSessionHandshakeInterceptor를 통해 HTTP 세션이 WebSocket 세션으로 복사됩니다.
     * 사용자 인증 정보는 HTTP 세션에서 자동으로 전달됩니다.
     *
     * @param stompClient WebSocket STOMP 클라이언트
     * @param wsUrl WebSocket 엔드포인트 URL
     * @param jsessionId HTTP 세션 ID (JSESSIONID 쿠키 값). null인 경우 쿠키 없이 연결
     * @return STOMP 세션
     */
    private static StompSession connectToWebSocket(
            final WebSocketStompClient stompClient,
            final String wsUrl,
            final String jsessionId
    ) throws ExecutionException, InterruptedException, TimeoutException {

        final WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        if (jsessionId != null && !jsessionId.isEmpty()) {
            httpHeaders.add("Cookie", "JSESSIONID=" + jsessionId);
        }

        final CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(
                wsUrl,
                httpHeaders,
                new StompHeaders(),
                new StompSessionHandlerAdapter() {
                }
        );

        return sessionFuture.get(2, TimeUnit.SECONDS);
    }

    /**
     * 특정 destination을 구독합니다.
     *
     * @param destination 구독할 destination 경로
     * @param messageType 수신할 메시지 타입
     * @return 현재 세션 인스턴스 (메서드 체이닝을 위해)
     */
    public TestStompSession<T> subscribe(final String destination, final Class<T> messageType) {
        final TestStompFrameHandler<T> frameHandler = new TestStompFrameHandler<>(messageType);
        session.subscribe(destination, frameHandler);
        subscriptions.put(destination, frameHandler);

        return this;
    }

    /**
     * 특정 destination으로 메시지를 전송합니다.
     *
     * @param destination 메시지를 전송할 destination 경로
     * @param payload 전송할 메시지 객체
     */
    public void send(final String destination, final Object payload) {
        session.send(destination, payload);
    }

    /**
     * 특정 destination에서 수신한 메시지 목록을 반환합니다.
     *
     * @param destination 메시지를 조회할 destination 경로
     * @return 수신한 메시지 목록
     */
    public List<T> getReceivedMessages(final String destination) {
        final TestStompFrameHandler<T> frameHandler = subscriptions.get(destination);
        if (frameHandler == null) {
            return List.of();
        }
        return frameHandler.getMessages();
    }

    /**
     * WebSocket 연결을 종료합니다.
     */
    public void disconnect() {
        session.disconnect();
        stompClient.stop();
        subscriptions.clear();
    }
}
