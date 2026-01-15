package performance.endpoints;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * 라이브 스트리밍 V2 API 엔드포인트 정의 (폴링 방식)
 * <p>
 * WebSocket 대신 HTTP 폴링을 사용하여 실시간 데이터를 조회합니다.
 * 재사용 가능한 HTTP 요청을 정적 필드로 제공합니다.
 */
public class LiveStreamingV2Endpoints {

    private LiveStreamingV2Endpoints() {
    }

    /**
     * 라이브 스트리밍 메타데이터 조회 엔드포인트 (인증 불필요)
     * <p>
     * 공개 API로, 로그인 없이 라이브 스트리밍 메타데이터를 조회할 수 있습니다.
     */
    public static final HttpRequestActionBuilder getLiveStreamMetadata = http("라이브 스트리밍 메타데이터 조회 (V2)")
            .get("/api/v2/livestreams/1/metadata")
            .check(status().is(200));

    /**
     * 실시간 통계 조회 + Heartbeat 업데이트 엔드포인트
     * <p>
     * Side Effect: 이 GET 요청은 사용자의 heartbeat를 기록하여 실시간 시청자 수에 영향을 줍니다.
     * 폴링 효율성을 위해 조회와 heartbeat를 결합했습니다.
     * <p>
     * 세션 정보를 통해 clientId와 userId를 자동으로 전달합니다.
     */
    public static final HttpRequestActionBuilder pollLiveStats = http("실시간 통계 폴링 + Heartbeat")
            .get("/api/v2/livestreams/1/live-stats")
            .check(status().is(200));

    /**
     * 초기 채팅 조회 엔드포인트
     * <p>
     * lastChatId 없이 호출하여 최초 채팅 목록을 가져옵니다.
     * 응답의 lastChatId를 세션에 저장하여 이후 폴링에 사용합니다.
     * <p>
     * lastChatId가 null일 수 있으므로 optional()을 사용합니다.
     */
    public static final HttpRequestActionBuilder getInitialChats = http("초기 채팅 조회")
            .get("/api/v2/livestreams/1/chats")
            .check(status().is(200))
            .check(jsonPath("$.lastChatId").optional().saveAs("lastChatId"));

    /**
     * 새로운 채팅 조회 엔드포인트
     * <p>
     * lastChatId를 파라미터로 전달하여 이후 채팅만 가져옵니다.
     * 세션의 lastChatId 값을 사용합니다.
     * 응답의 lastChatId로 세션값을 업데이트하여 다음 폴링에 사용합니다.
     * <p>
     * lastChatId가 세션에 없거나 null이면 쿼리 파라미터 없이 호출하여 초기 채팅을 가져옵니다.
     * lastChatId가 null일 수 있으므로 optional()을 사용합니다.
     */
    public static final HttpRequestActionBuilder pollNewChats = http("새 채팅 폴링")
            .get(session -> {
                final String baseUrl = "/api/v2/livestreams/1/chats";
                final String lastChatId = session.getString("lastChatId");
                // lastChatId가 있고, "null" 문자열이 아니며, 비어있지 않을 때만 쿼리 파라미터 추가
                if (lastChatId != null && !lastChatId.equals("null") && !lastChatId.isEmpty()) {
                    return baseUrl + "?lastChatId=" + lastChatId;
                }
                return baseUrl;
            })
            .check(status().is(200))
            .check(jsonPath("$.lastChatId").optional().saveAs("lastChatId"));

    /**
     * 채팅 메시지 전송 엔드포인트 (인증 필요)
     * <p>
     * 세션이 있는 인증 사용자가 채팅 메시지를 전송합니다.
     * 세션의 chatMessage 값을 사용합니다.
     */
    public static final HttpRequestActionBuilder sendChatMessage = http("채팅 메시지 전송")
            .post("/api/v2/livestreams/1/chats")
            .body(StringBody("""
                    {
                        "message": "#{chatMessage}",
                        "chatMessageType": "CHAT"
                    }
                    """))
            .check(status().is(201));

    /**
     * 좋아요 토글 엔드포인트 (인증 필요)
     * <p>
     * 세션이 있는 인증 사용자가 좋아요를 토글합니다.
     * V1 API 사용 (V2에는 좋아요/싫어요 엔드포인트가 없음)
     */
    public static final HttpRequestActionBuilder toggleLike = http("좋아요 선택 (V1)")
            .post("/api/v1/livestreams/1/likes")
            .body(StringBody("""
                    {
                        "reactionType": "LIKE"
                    }
                    """))
            .check(status().is(200));

    /**
     * 싫어요 토글 엔드포인트 (인증 필요)
     * <p>
     * 세션이 있는 인증 사용자가 싫어요를 토글합니다.
     * V1 API 사용 (V2에는 좋아요/싫어요 엔드포인트가 없음)
     */
    public static final HttpRequestActionBuilder toggleDislike = http("싫어요 선택 (V1)")
            .post("/api/v1/livestreams/1/likes")
            .body(StringBody("""
                    {
                        "reactionType": "DISLIKE"
                    }
                    """))
            .check(status().is(200));
}
