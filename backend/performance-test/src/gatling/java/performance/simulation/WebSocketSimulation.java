package performance.simulation;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static performance.groups.AuthScenarioGroups.*;
import static performance.groups.WebSocketScenarioGroups.*;
import static performance.utils.Config.*;
import static performance.utils.LoadProfiles.spike;
import static performance.utils.Protocols.httpProtocol;
import static performance.utils.TestDataFeeder.createBehaviorFeeder;
import static performance.utils.TestDataFeeder.createUserFeeder;

/**
 * WebSocket 연결 및 채팅 구독 성능 테스트 시뮬레이션
 * <p>
 * Phase 3: WebSocket 연결 및 구독 테스트
 * <p>
 * 인증/비인증 사용자 시나리오:
 * 1. 인증 사용자: 로그인 → WebSocket 연결 및 구독 → VU별 다른 시간 동안 연결 유지 → 종료
 * 2. 비인증 사용자: WebSocket 연결 및 구독 (로그인 없이) → VU별 다른 시간 동안 연결 유지 → 종료
 *
 * 실행 예시:
 * - 기본 (인증 5명, 비인증 5명): ./gradlew gatlingRun
 * - 인증 50명, 비인증 50명: ./gradlew gatlingRun -DauthVu=50 -DunauthVu=50
 * - 대규모 테스트: ./gradlew gatlingRun -DauthVu=500 -DunauthVu=500 -DmaxUsers=10000
 */
public class WebSocketSimulation extends Simulation {

    // 인증 사용자 시나리오: 로그인 → WebSocket 연결 및 구독 → VU별 다른 시간 동안 연결 유지 → 종료
    private static final ScenarioBuilder authenticatedScenario = scenario("인증 사용자 WebSocket 시나리오")
            .feed(createUserFeeder(MAX_USERS))
            .feed(createBehaviorFeeder(AUTH_MIN_DURATION, AUTH_MAX_DURATION))
            .exec(
                    authenticate,
                    connectAndSubscribe,
                    keepAlive,
                    disconnect
            );

    // 비인증 사용자 시나리오: WebSocket 연결 및 구독 (로그인 없이) → VU별 다른 시간 동안 연결 유지 → 종료
    private static final ScenarioBuilder unauthenticatedScenario = scenario("비인증 사용자 WebSocket 시나리오")
            .feed(createBehaviorFeeder(UNAUTH_MIN_DURATION, UNAUTH_MAX_DURATION))
            .exec(
                    connectAndSubscribe,
                    keepAlive,
                    disconnect
            );

    // 부하 주입 프로파일 설정 및 테스트 실행
    {
        setUp(
                authenticatedScenario.injectOpen(spike(AUTH_VU)),
                unauthenticatedScenario.injectOpen(spike(UNAUTH_VU))
        )
                .assertions(global().failedRequests().count().lt(1L))
                .protocols(httpProtocol);
    }
}