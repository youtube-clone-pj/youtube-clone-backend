package performance.endpoints;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * 라이브 스트리밍 V1 API 엔드포인트 정의
 * <p>
 * 재사용 가능한 HTTP 요청을 정적 필드로 제공합니다.
 */
public class LiveStreamingV1Endpoints {

    private LiveStreamingV1Endpoints() {
    }

    /**
     * 라이브 스트리밍 메타데이터 조회 엔드포인트 (인증 불필요)
     * <p>
     * 공개 API로, 로그인 없이 라이브 스트리밍 메타데이터를 조회할 수 있습니다.
     */
    public static final HttpRequestActionBuilder getLiveStreamMetadata = http("라이브 스트리밍 메타데이터 조회")
            .get("/api/v1/livestreams/1/metadata")
            .check(status().is(200));

}
