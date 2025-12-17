package performance.simulation;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static performance.utils.Protocols.httpProtocol;
import static performance.utils.SessionManager.initializeSessionFile;
import static performance.utils.SessionManager.saveSession;
import static performance.utils.TestDataFeeder.createUserFeeder;

/**
 * Phase 5: 세션 초기화 시뮬레이션
 * <p>
 * 라이브 스트리밍 성능 테스트 실행 전에 테스트 사용자 세션을 미리 생성합니다.
 * 이 과정은 실제 성능 테스트의 부하 주입 대상이 아닙니다.
 * <p>
 * 목적:
 * - 실제 유튜브처럼 이미 로그인된 상태(자동로그인)를 시뮬레이션
 * - 로그인 과정을 성능 테스트에서 제외하여 라이브 스트리밍 부하만 측정
 * <p>
 * 동작 방식:
 * 1. 모든 테스트 사용자(loadtest1@test.com ~ loadtestN@test.com)로 로그인
 * 2. 각 사용자의 JSESSIONID 쿠키 획득
 * 3. (userId, email, username, JSESSIONID) 정보를 CSV 파일로 저장
 * <p>
 * 실행 예시:
 * <pre>
 * # 소규모 테스트용 세션 생성 (100명)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.SessionSetupSimulation \
 *   -DtotalUsers=100
 *
 * # 중규모 테스트용 세션 생성 (1,000명)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.SessionSetupSimulation \
 *   -DtotalUsers=1000
 *
 * # 대규모 테스트용 세션 생성 (50,000명)
 * ./gradlew :performance-test:gatlingRun \
 *   --simulation=performance.simulation.SessionSetupSimulation \
 *   -DtotalUsers=50000
 * </pre>
 * <p>
 * 주의사항:
 * - 테스트 환경 DB에 loadtest1@test.com ~ loadtestN@test.com 사용자가 미리 존재해야 함
 * - 모든 사용자의 비밀번호: password123
 * - 생성된 세션은 backend/performance-test/sessions/sessions.csv에 저장됨 (덮어쓰기)
 */
public class SessionSetupSimulation extends Simulation {

    // ==================== 설정 파라미터 ====================

    /**
     * 총 세션 생성 대상 사용자 수 (기본값: 100명)
     * <p>
     * 시스템 프로퍼티로 조절 가능: -DtotalUsers=1000
     */
    private static final int TOTAL_USERS = Integer.getInteger("totalUsers", 100);

    /**
     * 세션 생성 속도 제어 (초당 생성 수, 기본값: 100)
     * <p>
     * 너무 빠르게 생성하면 서버에 부하가 걸릴 수 있으므로 제어
     * 시스템 프로퍼티로 조절 가능: -DcreationRate=50
     */
    private static final int CREATION_RATE = Integer.getInteger("creationRate", 100);

    /**
     * 세션 생성 지속 시간 (초, 기본값: 자동 계산)
     * <p>
     * TOTAL_USERS / CREATION_RATE로 자동 계산
     * 예: 10,000명 / 100명/초 = 100초
     */
    private static final int CREATION_DURATION = Math.max(1, TOTAL_USERS / CREATION_RATE);

    // ==================== 시나리오 정의 ====================

    /**
     * 세션 초기화 시나리오
     * <p>
     * 실행 흐름:
     * 1. 사용자 정보 주입 (email, password)
     * 2. 로그인 API 호출
     * 3. JSESSIONID 쿠키 획득
     * 4. userId, username 추출
     * 5. 세션 정보를 CSV 파일로 저장
     */
    private static final ScenarioBuilder setupScenario = scenario("세션 초기화")
            .feed(createUserFeeder(TOTAL_USERS))
            .exec(
                    http("로그인")
                            .post("/api/auth/login")
                            .body(StringBody("""
                                    {
                                        "email": "#{email}",
                                        "password": "#{password}"
                                    }
                                    """))
                            .check(status().is(200))
                            .check(jmesPath("userId").saveAs("userId"))
                            .check(jmesPath("username").saveAs("username"))
                            .check(headerRegex("Set-Cookie", "JSESSIONID=([^;]+)").saveAs("sessionId"))
            )
            .exec(session -> {
                // 세션 정보 저장
                final String userId = session.getString("userId");
                final String email = session.getString("email");
                final String sessionId = session.getString("sessionId");
                final String username = session.getString("username");

                saveSession(userId, email, username, sessionId);

                return session;
            });

    // ==================== 테스트 설정 ====================

    {
        System.out.println("========================================");
        System.out.println("  세션 초기화 시작");
        System.out.println("========================================");
        System.out.println("총 사용자 수: " + TOTAL_USERS);
        System.out.println("생성 속도: " + CREATION_RATE + "명/초");
        System.out.println("예상 소요 시간: " + CREATION_DURATION + "초");
        System.out.println("저장 위치: sessions/sessions.csv");
        System.out.println("========================================");

        // 세션 파일 초기화 (덮어쓰기)
        initializeSessionFile();

        setUp(
                setupScenario.injectOpen(
                        constantUsersPerSec(CREATION_RATE).during(CREATION_DURATION)
                )
        )
                .assertions(
                        // 모든 로그인이 성공해야 함
                        global().failedRequests().count().is(0L),
                        // 응답 시간 < 1초
                        global().responseTime().percentile3().lt(1000)
                )
                .protocols(httpProtocol);
    }
}
