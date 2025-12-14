package performance.endpoints;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * 인증 관련 API 엔드포인트 정의
 * <p>
 * 재사용 가능한 HTTP 요청을 정적 필드로 제공합니다.
 */
public class AuthEndpoints {

    private AuthEndpoints() {
    }

    /**
     * 로그인 엔드포인트
     * <p>
     * 세션에서 email, password를 주입받아 로그인을 수행합니다.
     * 응답으로 받은 userId, username을 세션에 저장합니다.
     */
    public static final HttpRequestActionBuilder login = http("로그인")
            .post("/api/auth/login")
            .body(StringBody("""
                    {
                        "email": "#{email}",
                        "password": "#{password}"
                    }
                    """))
            .check(status().is(200))
            .check(jsonPath("$.userId").saveAs("userId"))
            .check(jsonPath("$.username").saveAs("username"));


}
