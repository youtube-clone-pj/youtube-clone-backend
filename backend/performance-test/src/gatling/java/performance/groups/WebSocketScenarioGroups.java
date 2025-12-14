package performance.groups;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.group;
import static performance.endpoints.WebSocketEndpoints.*;
import static performance.utils.Config.LIVESTREAM_ID;

/**
 * WebSocket 테스트 시나리오 그룹
 *
 * Gatling의 group() API를 활용하여 논리적 단위로 그룹화합니다.
 * 이를 통해 Gatling 리포트에서 각 그룹별 통계를 확인할 수 있습니다.
 *
 * 각 그룹은 독립적인 ChainBuilder로 구성되어 재사용 및 재조합이 가능합니다.
 */
public class WebSocketScenarioGroups {

    private WebSocketScenarioGroups() {
    }

    /**
     * WebSocket 연결 및 구독 그룹
     *
     * 실행 흐름:
     * 1. Raw WebSocket 연결 (ws://localhost:8080/ws-direct)
     * 2. STOMP CONNECT 프레임 전송 및 CONNECTED 응답 수신
     * 3. STOMP SUBSCRIBE 프레임 전송 및 초기 메시지 수신
     */
    public static final ChainBuilder connectAndSubscribe =
            group("WebSocket 연결 및 구독").on(
                    connect,
                    stompConnect,
                    stompSubscribe(LIVESTREAM_ID)
            );

    /**
     * 연결 유지 그룹
     *
     * STOMP Heartbeat를 전송하며 WebSocket 연결을 유지합니다.
     * 세션의 "sessionDuration" 속성에서 연결 유지 시간을 읽어옵니다.
     *
     * 실행 흐름:
     * - 10초마다 heartbeat (빈 줄 "\n") 전송
     * - 각 VU별로 다른 sessionDuration 동안 계속 반복
     */
    public static final ChainBuilder keepAlive =
            group("연결 유지").on(
                    keepAliveWithHeartbeatFromSession
            );

    /**
     * 연결 종료 그룹
     *
     * STOMP DISCONNECT 프레임을 전송하고 WebSocket 연결을 종료합니다.
     */
    public static final ChainBuilder disconnect =
            group("연결 종료").on(
                    disconnectWebSocket
            );
}