package performance.simulation;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static performance.groups.AuthScenarioGroups.authenticate;
import static performance.groups.WebSocketScenarioGroups.*;
import static performance.utils.ChatTestDataFeeder.createChatBehaviorFeeder;
import static performance.utils.Config.MAX_USERS;
import static performance.utils.LoadProfiles.spike;
import static performance.utils.Protocols.httpProtocol;
import static performance.utils.TestDataFeeder.createBehaviorFeeder;
import static performance.utils.TestDataFeeder.createUserFeeder;

/**
 * Phase 4: 채팅 메시지 전송 및 수신 성능 테스트 시뮬레이션
 * <p>
 * 인증/비인증 사용자가 라이브 스트리밍에 접속하여 채팅 메시지를 전송하고 수신하는 시나리오를 테스트합니다.
 * <p>
 * 시나리오 흐름:
 * 1. WebSocket 연결 및 구독
 * 2. pause(1~3초) - 화면 로딩 시뮬레이션
 * 3. 초기 활발한 채팅 (5분간)
 * 4. 안정화된 채팅 (나머지 시간)
 * 5. 연결 종료
 * <p>
 * 실행 예시:
 * - 소규모 테스트 (10명, 약 1분): ./gradlew gatlingRun -DauthVu=5 -DunauthVu=5 -DminDuration=360 -DmaxDuration=420
 * - 중규모 테스트 (100명, 5분): ./gradlew gatlingRun -DauthVu=50 -DunauthVu=50 -DminDuration=600 -DmaxDuration=900
 */
public class ChatSimulation extends Simulation {

    // 채팅 행동 패턴 설정 (최소/최대 세션 시간)
    // 기본값: 6분(360초) ~ 7분(420초)
    // 초기 활발한 채팅 5분 + 안정화된 채팅 1~2분
    private static final int MIN_SESSION_DURATION = Integer.getInteger("minDuration", 360);
    private static final int MAX_SESSION_DURATION = Integer.getInteger("maxDuration", 420);

    // 가상 사용자 수 설정 (기본값: 인증 5명, 비인증 5명)
    private static final int AUTH_VU = Integer.getInteger("authVu", 5);
    private static final int UNAUTH_VU = Integer.getInteger("unauthVu", 5);

    /**
     * 인증 사용자 채팅 시나리오
     * <p>
     * 실행 흐름:
     * 1. 로그인
     * 2. WebSocket 연결 및 구독
     * 3. pause(1~3초) - 화면 로딩
     * 4. 초기 활발한 채팅 (5분)
     * 5. 안정화된 채팅 (나머지 시간)
     * 6. 연결 종료
     */
    private static final ScenarioBuilder authenticatedChatScenario = scenario("인증 사용자 채팅 시나리오")
            .feed(createUserFeeder(MAX_USERS))
            .feed(createChatBehaviorFeeder(MIN_SESSION_DURATION, MAX_SESSION_DURATION))
            .exec(
                    authenticate,
                    connectAndSubscribe,
                    pause(1, 3),
                    sendInitialChats,
                    sendNormalChats,
                    disconnect
            );

    /**
     * 비인증 사용자 시나리오
     * <p>
     * 비인증 사용자는 채팅을 읽을 수만 있고 전송할 수 없습니다.
     * <p>
     * 실행 흐름:
     * 1. WebSocket 연결 및 구독 (로그인 없이)
     * 2. pause(1~3초) - 화면 로딩
     * 3. 연결 유지 (세션 시간 동안 다른 사용자의 채팅 수신)
     * 4. 연결 종료
     */
    private static final ScenarioBuilder unauthenticatedChatScenario = scenario("비인증 사용자 시나리오")
            .feed(createBehaviorFeeder(MIN_SESSION_DURATION, MAX_SESSION_DURATION))
            .exec(
                    connectAndSubscribe,
                    pause(1, 3),
                    keepAlive,
                    disconnect
            );

    // 부하 주입 프로파일 설정 및 테스트 실행
    {
        setUp(
                authenticatedChatScenario.injectOpen(spike(AUTH_VU)),
                unauthenticatedChatScenario.injectOpen(spike(UNAUTH_VU))
        )
                .assertions(
                        global().failedRequests().count().lt(1L),
                        global().responseTime().percentile3().lt(1000)
                )
                .protocols(httpProtocol);
    }
}