package performance.simulation.livestreaming;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.Cookie;
import static io.gatling.javaapi.http.HttpDsl.addCookie;
import static performance.groups.LiveStreamingScenarioGroups.*;
import static performance.groups.WebSocketScenarioGroups.*;
import static performance.utils.Protocols.httpProtocol;
import static performance.utils.SessionManager.loadSessionFeeder;
import static performance.utils.TestDataFeeder.createBehaviorFeeder;

/**
 * Phase 5: 라이브 스트리밍 WebSocket 방식 부하 테스트
 * <p>
 * WebSocket을 사용하여 실시간 데이터를 수신하는 성능 테스트입니다.
 * 5만 명의 점진적 유입을 시뮬레이션하여 시스템의 한계를 파악하고
 * 병목 지점을 식별하는 것을 목표로 합니다.
 * <p>
 * 전제 조건:
 * - SessionSetupSimulation을 먼저 실행하여 세션 파일을 생성해야 함
 * - 세션 파일 위치: src/gatling/java/performance/simulation/session/sessions.csv
 * - 생성된 세션 파일의 사용자 수 >= TOTAL_USERS * AUTH_RATIO
 * <p>
 * 단계적 부하 증가 전략:
 * 1. 소규모 테스트 (100명) - 시나리오 동작 확인
 * 2. 중규모 테스트 (1,000명) - 병목 지점 초기 파악
 * 3. 대규모 테스트 (10,000명) - 시스템 한계 탐색
 * 4. 목표 테스트 (50,000명) - 실제 시나리오 시뮬레이션
 * 5. 스트레스 테스트 (75,000~100,000명) - 시스템 한계점 확인 (선택)
 * <p>
 * 부하 주입 패턴 (realistic):
 * - 인기 스트리머의 라이브 스트리밍 시작 시 발생하는 실제 트래픽 패턴을 시뮬레이션
 * - 첫 1분: 0 → 초당 300명으로 급증 (알림을 받고 바로 접속)
 * - 1~2분: 초당 300명 유지 (피크 구간)
 * - 2~4분: 초당 300명 → 100명으로 감소 (점진적 안정화)
 * - 4~5분: 초당 100명 유지 (안정화 구간)
 * <p>
 * 실행 예시:
 * <pre>
 * # 0단계: 세션 생성 (최초 1회만 실행)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.session.SessionSetupSimulation \
 *   -DtotalUsers=50000
 *
 * # 1단계: 소규모 테스트 (100명)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation \
 *   -DtotalUsers=100
 *
 * # 2단계: 중규모 테스트 (1,000명)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation \
 *   -DtotalUsers=1000
 *
 * # 3단계: 대규모 테스트 (10,000명)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation \
 *   -DtotalUsers=10000
 *
 * # 4단계: 목표 테스트 (50,000명)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation \
 *   -DtotalUsers=50000
 *
 * # 5단계: 스트레스 테스트 (100,000명)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation \
 *   -DtotalUsers=100000
 * </pre>
 */
public class LiveStreamingLoadTestSimulation extends Simulation {

    // ==================== 설정 파라미터 ====================

    /**
     * 총 가상 사용자 수 (기본값: 100명)
     * <p>
     * 시스템 프로퍼티로 조절 가능: -DtotalUsers=1000
     */
    private static final int TOTAL_USERS = Integer.getInteger("totalUsers", 100);

    /**
     * 인증 사용자 비율 (0.0 ~ 1.0, 기본값: 0.7 = 70%)
     * <p>
     * 시스템 프로퍼티로 조절 가능: -DauthRatio=0.8
     */
    private static final double AUTH_RATIO = Double.parseDouble(System.getProperty("authRatio", "0.7"));

    /**
     * 최소 세션 지속 시간 (초, 기본값: 360초 = 6분)
     * <p>
     * 시스템 프로퍼티로 조절 가능: -DminDuration=600
     */
    private static final int MIN_SESSION_DURATION = Integer.getInteger("minDuration", 360);

    /**
     * 최대 세션 지속 시간 (초, 기본값: 420초 = 7분)
     * <p>
     * 시스템 프로퍼티로 조절 가능: -DmaxDuration=900
     */
    private static final int MAX_SESSION_DURATION = Integer.getInteger("maxDuration", 420);

    // 계산된 값
    private static final int AUTH_USERS = (int) (TOTAL_USERS * AUTH_RATIO);
    private static final int UNAUTH_USERS = TOTAL_USERS - AUTH_USERS;

    // ==================== 시나리오 정의 ====================

    /**
     * 인증 사용자 시나리오 (WebSocket 방식)
     * <p>
     * 실행 흐름:
     * 1. 세션 파일에서 사전 생성된 세션 로드 (userId, email, username, sessionId)
     * 2. JSESSIONID 쿠키 설정 (이미 로그인된 상태)
     * 3. 메타데이터 조회 (라이브 스트리밍 정보 확인)
     * 4. WebSocket 연결 및 구독 (채팅, 시청자 수, 좋아요 수 브로드캐스트 수신)
     * 5. pause(1~3초) - 화면 로딩
     * 6. 좋아요/싫어요 반응 (10% 확률로 반응, 그 중 95% 좋아요, 5% 싫어요)
     * 7. 세션 동안 채팅 전송 (초기 5분: 10% 확률, 이후: 4% 확률)
     * 8. 연결 종료
     */
    private static final ScenarioBuilder authenticatedScenario = scenario("인증 사용자 라이브 스트리밍 시나리오 (WebSocket)")
            .feed(loadSessionFeeder())
            .feed(createBehaviorFeeder(MIN_SESSION_DURATION, MAX_SESSION_DURATION))
            .exec(
                    addCookie(Cookie("JSESSIONID", "#{sessionId}").withPath("/")),
                    fetchMetadata,
                    connectAndSubscribe,
                    pause(1, 3),
                    reactToStream,
                    authenticatedUserWebSocketBehavior,
                    disconnect
            );

    /**
     * 비인증 사용자 시나리오 (WebSocket 방식)
     * <p>
     * 실행 흐름:
     * 1. 메타데이터 조회 (라이브 스트리밍 정보 확인, 로그인 없이)
     * 2. WebSocket 연결 및 구독 (채팅, 시청자 수, 좋아요 수 브로드캐스트 수신)
     * 3. pause(1~3초) - 화면 로딩
     * 4. 연결 유지 (세션 시간 동안 다른 사용자의 채팅 수신)
     * 5. 연결 종료
     */
    private static final ScenarioBuilder unauthenticatedScenario = scenario("비인증 사용자 라이브 스트리밍 시나리오 (WebSocket)")
            .feed(createBehaviorFeeder(MIN_SESSION_DURATION, MAX_SESSION_DURATION))
            .exec(
                    fetchMetadata,
                    connectAndSubscribe,
                    pause(1, 3),
                    keepAlive,
                    disconnect
            );

    // ==================== 부하 주입 프로파일 ====================

    /**
     * 부하 주입 프로파일 생성
     * <p>
     * realistic 패턴을 사용하여 실제 트래픽 패턴을 시뮬레이션합니다.
     *
     * @param userCount 사용자 수
     * @return 부하 주입 스텝 배열
     */
    private static OpenInjectionStep[] createInjectionProfile(final int userCount) {
        return createRealisticProfile(userCount);
    }

    /**
     * 현실적인 부하 주입 패턴 생성
     * <p>
     * 인기 스트리머의 라이브 스트리밍 시작 시 발생하는 실제 트래픽 패턴을 시뮬레이션합니다:
     * - 첫 1분: 0 → 초당 300명으로 급증 (알림을 받고 바로 접속)
     * - 1~2분: 초당 300명 유지 (피크 구간)
     * - 2~4분: 초당 300명 → 100명으로 감소 (점진적 안정화)
     * - 4~5분: 초당 100명 유지 (안정화 구간)
     *
     * @param totalUsers 총 사용자 수
     * @return 부하 주입 스텝 배열
     */
    private static OpenInjectionStep[] createRealisticProfile(final int totalUsers) {
        // 총 5분(300초)에 걸쳐 사용자 유입
        // 1분(60초): 0 → 300명/초로 증가 = 평균 150명/초 × 60초 = 9,000명
        // 1분(60초): 300명/초 유지 = 18,000명
        // 2분(120초): 300 → 100명/초로 감소 = 평균 200명/초 × 120초 = 24,000명
        // 1분(60초): 100명/초 유지 = 6,000명
        // 합계: 약 57,000명 (5만 명 목표에 근접)

        // 비율로 계산
        final double scale = totalUsers / 57000.0;
        final double peakRate = 300 * scale;
        final double stabilizedRate = 100 * scale;

        return new OpenInjectionStep[]{
                nothingFor(0),
                rampUsersPerSec(0).to(peakRate).during(60),      // 첫 1분: 급증
                constantUsersPerSec(peakRate).during(60),        // 1~2분: 피크 유지
                rampUsersPerSec(peakRate).to(stabilizedRate).during(120), // 2~4분: 점진 감소
                constantUsersPerSec(stabilizedRate).during(60)   // 4~5분: 안정화
        };
    }

    // ==================== 테스트 설정 ====================

    {
        System.out.println("========================================");
        System.out.println("  라이브 스트리밍 WebSocket 부하 테스트 시작");
        System.out.println("========================================");
        System.out.println("총 사용자 수: " + TOTAL_USERS);
        System.out.println("인증 사용자: " + AUTH_USERS + " (" + (int) (AUTH_RATIO * 100) + "%)");
        System.out.println("비인증 사용자: " + UNAUTH_USERS + " (" + (int) ((1 - AUTH_RATIO) * 100) + "%)");
        System.out.println("부하 주입 패턴: realistic (초기 급증 + 점진 감소)");
        System.out.println("세션 지속 시간: " + MIN_SESSION_DURATION + "~" + MAX_SESSION_DURATION + "초");
        System.out.println("세션 파일: " + performance.utils.SessionManager.getSessionFilePath());
        System.out.println("통신 방식: WebSocket (STOMP)");
        System.out.println("========================================");

        setUp(
                authenticatedScenario.injectOpen(createInjectionProfile(AUTH_USERS)),
                unauthenticatedScenario.injectOpen(createInjectionProfile(UNAUTH_USERS))
        )
                .assertions(
                        // ========== 전역 검증 ==========
                        // 에러율 < 1%
                        global().failedRequests().percent().lt(1.0),

                        // 입장 시 한 번만 호출
                        details("메타데이터 조회", "라이브 스트리밍 메타데이터 조회").responseTime().percentile(95.0).lt(300),
                        details("메타데이터 조회", "라이브 스트리밍 메타데이터 조회").responseTime().percentile(99.0).lte(600),

                        // WebSocket SEND부터 브로드캐스트 수신까지 (확률 기반 채팅)
                        details("인증 사용자 행동 (확률 기반 채팅)", "채팅 메시지 전송 및 브로드캐스트 수신").responseTime().percentile(95.0).lt(150),
                        details("인증 사용자 행동 (확률 기반 채팅)", "채팅 메시지 전송 및 브로드캐스트 수신").responseTime().percentile(99.0).lte(300),

                        // WebSocket 구독 시 초기 메시지 수신
                        details("WebSocket 연결 및 구독", "초기 메시지 확인").responseTime().percentile(95.0).lt(500),
                        details("WebSocket 연결 및 구독", "초기 메시지 확인").responseTime().percentile(99.0).lte(1000),

                        // 인증 사용자의 좋아요/싫어요
                        details("좋아요/싫어요 반응", "좋아요 선택").responseTime().percentile(95.0).lt(600),
                        details("좋아요/싫어요 반응", "좋아요 선택").responseTime().percentile(99.0).lte(1200),
                        details("좋아요/싫어요 반응", "싫어요 선택").responseTime().percentile(95.0).lt(600),
                        details("좋아요/싫어요 반응", "싫어요 선택").responseTime().percentile(99.0).lte(1200)
                )
                .protocols(httpProtocol);
    }
}