package performance.utils;

/**
 * STOMP 프레임을 생성하는 유틸리티 클래스
 * <p>
 * STOMP 프로토콜 1.2 스펙에 따라 프레임을 생성합니다.
 * NULL 문자(\u0000)로 프레임을 종료합니다.
 *
 * @see <a href="https://stomp.github.io/stomp-specification-1.2.html">STOMP 1.2 Specification</a>
 */
public class StompFrameBuilder {

    private static final String NULL_CHAR = "\u0000";

    /**
     * STOMP CONNECT 프레임 생성
     * <p>
     * 클라이언트가 서버에 연결할 때 전송하는 프레임입니다.
     * heartbeat 설정을 포함합니다.
     *
     * @return STOMP CONNECT 프레임
     */
    public static String connect() {
        return """
                CONNECT
                accept-version:1.2
                heart-beat:10000,10000

                """ + NULL_CHAR;
    }

    /**
     * STOMP SUBSCRIBE 프레임 생성
     * <p>
     * 특정 목적지(destination)를 구독하는 프레임입니다.
     *
     * @param subscriptionId 구독 ID (예: sub-0)
     * @param destination    구독할 목적지 (예: /topic/livestreams/1/chat/messages)
     * @return STOMP SUBSCRIBE 프레임
     */
    public static String subscribe(final String subscriptionId, final String destination) {
        return String.format("""
                SUBSCRIBE
                id:%s
                destination:%s

                %s""", subscriptionId, destination, NULL_CHAR);
    }

    /**
     * STOMP SEND 프레임 생성
     * <p>
     * 서버로 메시지를 전송하는 프레임입니다.
     *
     * @param destination 전송할 목적지 (예: /app/livestreams/1/chat/messages)
     * @param body        전송할 메시지 본문 (JSON 형식)
     * @return STOMP SEND 프레임
     */
    public static String send(final String destination, final String body) {
        return String.format("""
                SEND
                destination:%s
                content-type:application/json
                content-length:%d

                %s%s""", destination, body.length(), body, NULL_CHAR);
    }

    /**
     * STOMP DISCONNECT 프레임 생성
     * <p>
     * 서버와의 연결을 종료하는 프레임입니다.
     *
     * @return STOMP DISCONNECT 프레임
     */
    public static String disconnect() {
        return "DISCONNECT\n\n" + NULL_CHAR;
    }

    private StompFrameBuilder() {
        // 유틸리티 클래스: 인스턴스화 방지
    }
}