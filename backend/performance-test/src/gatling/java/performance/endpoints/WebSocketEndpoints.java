package performance.endpoints;

import io.gatling.javaapi.core.ChainBuilder;
import performance.utils.StompFrameBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.ws;

/**
 * WebSocket 엔드포인트 (성능 테스트 전용)
 *
 * 프로덕션과 동일한 설정을 사용하되, SockJS 없이 순수 WebSocket + STOMP 프로토콜만 사용합니다.
 *
 * 프로토콜: STOMP 1.2 over Raw WebSocket
 * URL: ws://localhost:8080/ws-direct
 * Heartbeat: 10초 간격
 *
 * 각 엔드포인트는 독립적인 ChainBuilder로 구성되어 재조합 가능합니다.
 */
public class WebSocketEndpoints {

    private WebSocketEndpoints() {
    }

    /**
     * 1. Raw WebSocket 연결
     *
     * ws://localhost:8080/ws-direct로 WebSocket 연결을 수립합니다.
     */
    public static final ChainBuilder connect =
            exec(ws("Raw WebSocket 연결")
                    .connect("/ws-direct"));

    /**
     * 2. STOMP CONNECT 프레임 전송 및 CONNECTED 응답 수신
     *
     * 실행 흐름:
     * 1. STOMP CONNECT 프레임 생성
     * 2. 프레임 전송
     * 3. STOMP CONNECTED 프레임 수신 대기 (최대 10초)
     * 4. 연결 성공 확인
     */
    public static final ChainBuilder stompConnect =
            exec(session -> {
                final String connectFrame = StompFrameBuilder.connect();
                return session.set("connectFrame", connectFrame);
            })
                    .exec(
                            ws("STOMP CONNECT 전송")
                                    .sendText("#{connectFrame}")
                                    .await(10).on(
                                            ws.checkTextMessage("STOMP CONNECTED 확인")
                                                    .matching(regex("CONNECTED"))
                                                    .check(bodyString().saveAs("connectedFrame"))
                                    )
                    );

    /**
     * 3. STOMP SUBSCRIBE 프레임 전송 및 초기 메시지 수신
     *
     * - /app/livestreams/{id}/chat/messages → @SubscribeMapping 호출
     *
     * 실행 흐름:
     * 1. STOMP SUBSCRIBE 프레임 생성
     * 2. 프레임 전송
     * 3. InitialChatMessagesResponse (초기 메시지) 수신 대기 (최대 10초)
     *
     * @param livestreamId 구독할 라이브 스트리밍 ID
     * @return STOMP SUBSCRIBE ChainBuilder
     */
    public static ChainBuilder stompSubscribe(final Long livestreamId) {
        final String chatTopic = "/app/livestreams/" + livestreamId + "/chat/messages";
        final String subscriptionId = "sub-0";

        return exec(session -> {
            final String subscribeFrame = StompFrameBuilder.subscribe(subscriptionId, chatTopic);
            return session.set("subscribeFrame", subscribeFrame);
        })
                .exec(
                        ws("STOMP SUBSCRIBE 전송")
                                .sendText("#{subscribeFrame}")
                                .await(10).on(
                                        ws.checkTextMessage("초기 메시지 확인")
                                                .matching(regex("MESSAGE"))
                                                .check(bodyString().saveAs("initialMessages"))
                                )
                );
    }

    /**
     * 4. STOMP Heartbeat 전송 (단일)
     *
     * STOMP 프로토콜의 heartbeat는 빈 줄(\n)을 전송합니다.
     * 10초 대기 후 다음 heartbeat를 전송합니다.
     */
    public static final ChainBuilder heartbeat =
            exec(
                    ws("Heartbeat 전송")
                            .sendText("\n")
            )
                    .pause(10);

    /**
     * 5. STOMP Heartbeat 전송하며 WebSocket 연결 유지 (세션 기반)
     *
     * 세션의 "sessionDuration" 속성에서 연결 유지 시간을 읽어옵니다.
     * 각 가상 사용자(VU)마다 다른 세션 시간을 적용할 때 사용합니다.
     *
     * 실행 흐름:
     * - during("#{sessionDuration}")에서 Gatling EL로 세션의 sessionDuration 값을 읽음
     * - 해당 시간(초) 동안 10초마다 heartbeat 전송
     *
     * 사용 예시:
     * <pre>
     * scenario("VU별 다른 세션 시간")
     *     .feed(createBehaviorFeeder(300, 900))  // 5분~15분 랜덤
     *     .exec(connectAndSubscribeToChat(1L))
     *     .exec(keepAliveWithHeartbeatFromSession)  // 각 VU가 자신의 sessionDuration만큼 연결
     * </pre>
     */
    public static final ChainBuilder keepAliveWithHeartbeatFromSession =
            during("#{sessionDuration}").on(
                    exec(
                            ws("Heartbeat 전송")
                                    .sendText("\n")
                    )
                            .pause(10)
            );

    /**
     * 6. STOMP DISCONNECT 프레임 전송 및 WebSocket 연결 종료
     *
     * 실행 흐름:
     * 1. STOMP DISCONNECT 프레임 생성
     * 2. 프레임 전송
     * 3. WebSocket 연결 종료
     */
    public static final ChainBuilder disconnectWebSocket =
            exec(session -> {
                final String disconnectFrame = StompFrameBuilder.disconnect();
                return session.set("disconnectFrame", disconnectFrame);
            })
                    .exec(
                            ws("STOMP DISCONNECT 전송")
                                    .sendText("#{disconnectFrame}")
                    )
                    .exec(
                            ws("WebSocket 연결 종료")
                                    .close()
                    );
}
