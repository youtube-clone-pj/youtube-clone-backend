package performance.simulation;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static performance.endpoints.AuthEndpoints.login;
import static performance.endpoints.LiveStreamingV1Endpoints.getLiveStreamMetadata;
import static performance.utils.TestDataFeeder.createUserFeeder;

public class AuthSimulation extends Simulation {

    // 가상 사용자 수 (시스템 프로퍼티로 설정 가능, 기본값: 10)
    // 실행 예시: ./gradlew gatlingRun -Dvu=100
    private static final int vu = Integer.getInteger("vu", 10);

    // DB에 적재된 최대 테스트 사용자 수 (기본값: 10000)
    // 소규모 테스트 시: -DmaxUsers=10
    // 대규모 테스트 시: -DmaxUsers=10000
    private static final int maxUsers = Integer.getInteger("maxUsers", 2);

    // HTTP 프로토콜 설정
    private static final HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // 시나리오 정의: 사용자 데이터 주입 → 로그인 → 라이브 스트리밍 메타데이터 조회
    private static final ScenarioBuilder scenario = scenario("인증 및 세션 테스트")
            .feed(createUserFeeder(maxUsers))
            .exec(login)
            .exec(getLiveStreamMetadata);

    // 검증 조건: 실패한 요청이 1개 미만이어야 함
    private static final Assertion assertion = global().failedRequests().count().lt(1L);

    // 부하 주입 프로파일 설정 및 테스트 실행
    {
        setUp(scenario.injectOpen(atOnceUsers(vu)))
                .assertions(assertion)
                .protocols(httpProtocol);
    }
}