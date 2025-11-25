package com.youtube.api.testfixtures.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SSE(Server-Sent Events) 테스트를 위한 세션 래퍼 클래스
 * WebTestClient를 사용하여 SSE 연결을 관리하고 이벤트를 수신합니다.
 *
 * @param <T> 수신할 이벤트의 타입
 */
@Slf4j
public class TestSseSession<T> {

    private final ConcurrentLinkedQueue<T> receivedEvents = new ConcurrentLinkedQueue<>();
    private final Class<T> eventType;
    private final ObjectMapper objectMapper;
    private final String eventName;
    private Disposable subscription;

    private TestSseSession(
            final Class<T> eventType,
            final ObjectMapper objectMapper,
            final String eventName
    ) {
        this.eventType = eventType;
        this.objectMapper = objectMapper;
        this.eventName = eventName;
    }

    /**
     * SSE 연결을 생성합니다.
     *
     * @param baseUrl 서버 베이스 URL (예: http://localhost)
     * @param port 서버 포트
     * @param path SSE 엔드포인트 경로 (예: /api/v1/notifications/stream)
     * @param jsessionId HTTP 세션 ID (JSESSIONID 쿠키 값). null인 경우 쿠키 없이 연결
     * @param eventType 수신할 이벤트 타입
     * @param objectMapper JSON 파싱용 ObjectMapper
     * @param eventName 수신할 SSE 이벤트 이름 (예: "notification", "unread-count")
     * @param <T> 수신할 이벤트의 타입
     * @return 연결된 TestSseSession 인스턴스
     */
    public static <T> TestSseSession<T> connect(
            final String baseUrl,
            final int port,
            final String path,
            final String jsessionId,
            final Class<T> eventType,
            final ObjectMapper objectMapper,
            final String eventName
    ) {
        final TestSseSession<T> session = new TestSseSession<>(eventType, objectMapper, eventName);

        final WebTestClient webTestClient = WebTestClient.bindToServer()
                .baseUrl(baseUrl + ":" + port)
                .responseTimeout(Duration.ofSeconds(30))
                .build();

        final Flux<ServerSentEvent<String>> eventStream = webTestClient
                .get()
                .uri(path)
                .cookie("JSESSIONID", jsessionId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .getResponseBody();

        session.subscription = eventStream
                .filter(sse -> eventName.equals(sse.event()))
                .subscribe(
                        sse -> {
                            try {
                                final T event = objectMapper.readValue(
                                        sse.data(),
                                        eventType
                                );
                                session.receivedEvents.add(event);
                                log.debug("SSE 이벤트 수신 - eventName: {}, data: {}", eventName, sse.data());
                            } catch (Exception e) {
                                log.error("SSE 이벤트 파싱 실패 - error: {}", e.getMessage(), e);
                                throw new RuntimeException(e);
                            }
                        },
                        error -> {
                            if (!"Pending response has not been sent yet".equals(error.getMessage())) {
                                log.error("SSE 연결 오류 - error: {}", error.getMessage());
                            }
                        }
                );

        log.info("SSE 연결 성공 - URL: {}:{}{}, eventName: {}", baseUrl, port, path, eventName);
        return session;
    }

    public static <T> TestSseSession<T> connect(
            final int port,
            final String path,
            final String jsessionId,
            final Class<T> eventType,
            final ObjectMapper objectMapper,
            final String eventName
    ) {
        return connect("http://localhost", port, path, jsessionId, eventType, objectMapper, eventName);
    }

    public List<T> getReceivedEvents() {
        return new ArrayList<>(receivedEvents);
    }

    /**
     * @return 가장 최근 이벤트 (없으면 null)
     */
    public T getLatestEvent() {
        T latest = null;
        for (T event : receivedEvents) {
            latest = event;
        }
        return latest;
    }

    public void disconnect() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("SSE 연결 종료");
        }
    }
}
