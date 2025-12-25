package performance.groups;

import io.gatling.javaapi.core.ChainBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static performance.endpoints.WebSocketEndpoints.*;
import static performance.utils.ChatTestDataFeeder.createInitialChatMessageFeeder;
import static performance.utils.ChatTestDataFeeder.createNormalChatMessageFeeder;
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

    /**
     * 초기 활발한 채팅 그룹
     *
     * 라이브 스트리밍 시작 직후 사용자들이 전송하는 짧고 간단한 메시지들을 5분간 반복 전송합니다.
     *
     * 실행 흐름:
     * - 5분(300초) 동안 반복:
     *   1. 초기 채팅 메시지 Feeder에서 메시지 가져오기
     *   2. 채팅 메시지 전송 및 브로드캐스트 수신 (지연 시간 측정)
     *   3. 30~60초 대기 (랜덤)
     *
     * 참고: heartbeat timeout은 180초로 설정되어 있으므로 채팅 메시지 전송이 heartbeat 역할을 합니다.
     */
    public static final ChainBuilder sendInitialChats =
            group("초기 활발한 채팅 (5분)").on(
                    during(300).on(
                            feed(createInitialChatMessageFeeder())
                                    .exec(stompSendChatMessage(LIVESTREAM_ID))
                                    .pause(30, 60)
                    )
            );

    /**
     * 안정화된 채팅 그룹
     *
     * 라이브 스트리밍이 진행되면서 사용자들이 전송하는 일반적인 채팅 메시지를 반복 전송합니다.
     *
     * 실행 흐름:
     * - 세션의 normalChatDuration 동안 반복:
     *   1. 일반 채팅 메시지 Feeder에서 메시지 가져오기
     *   2. 채팅 메시지 전송 및 브로드캐스트 수신 (지연 시간 측정)
     *   3. 90~180초 대기 (랜덤)
     *
     * 주의: 세션에 normalChatDuration 값이 설정되어 있어야 합니다.
     *       createChatBehaviorFeeder()를 사용하면 자동으로 설정됩니다.
     */
    public static final ChainBuilder sendNormalChats =
            group("안정화된 채팅").on(
                    during("#{normalChatDuration}").on(
                            feed(createNormalChatMessageFeeder())
                                    .exec(stompSendChatMessage(LIVESTREAM_ID))
                                    .pause(90, 180)
                    )
            );

    /**
     * 인증 사용자의 전체 세션 동안의 행동 (확률 기반 채팅)
     * <p>
     * 채팅 전송을 확률 기반으로 수행하여 실제 사용자 행동 패턴을 시뮬레이션합니다.
     * <p>
     * 실행 흐름:
     * - 초기 5분: 10% 확률로 채팅 전송 (평균 40초에 한 번)
     * - 이후: 4% 확률로 채팅 전송 (평균 100초에 한 번)
     * - 채팅을 보내지 않을 때는 30~60초 대기
     */
    public static final ChainBuilder authenticatedUserWebSocketBehavior =
            group("인증 사용자 행동 (확률 기반 채팅)").on(
                    // 초기 5분 (10% 확률)
                    during(session -> Duration.ofSeconds(Math.min(300, session.getInt("sessionDuration")))).on(
                            randomSwitch().on(
                                    // 10% 확률로 채팅 전송
                                    percent(10.0).then(
                                            feed(createInitialChatMessageFeeder())
                                                    .exec(stompSendChatMessage(LIVESTREAM_ID))
                                    )
                            )
                            .pause(30, 60)
                    )
                    // 나머지 시간 (4% 확률)
                    .during(session -> Duration.ofSeconds(Math.max(0, session.getInt("sessionDuration") - 300))).on(
                            randomSwitch().on(
                                    // 4% 확률로 채팅 전송
                                    percent(4.0).then(
                                            feed(createNormalChatMessageFeeder())
                                                    .exec(stompSendChatMessage(LIVESTREAM_ID))
                                    )
                            )
                            .pause(90, 180)
                    )
            );
}