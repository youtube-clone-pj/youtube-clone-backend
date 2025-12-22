package performance.utils;

import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.http.HttpDsl.http;

/**
 * Gatling 테스트에서 사용하는 프로토콜 설정
 *
 * HTTP 및 WebSocket 프로토콜의 공통 설정을 중앙 관리합니다.
 */
public class Protocols {

    private Protocols() {
    }

    /**
     * HTTP 및 WebSocket 프로토콜 설정
     *
     * - HTTP 기본 URL: Config.BASE_URL
     * - WebSocket 기본 URL: Config.WS_BASE_URL
     * - Accept 헤더: application/json
     * - Content-Type 헤더: application/json
     */
    public static final HttpProtocolBuilder httpProtocol = http
            .baseUrl(Config.BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .wsBaseUrl(Config.WS_BASE_URL);
}