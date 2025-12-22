package performance.utils;

/**
 * Gatling 성능 테스트 중앙 설정 관리
 *
 * 모든 설정값은 시스템 프로퍼티로 런타임에 오버라이드 가능합니다.
 *
 * 실행 예시:
 * - 기본 설정: ./gradlew gatlingRun
 * - 사용자 수 변경: ./gradlew gatlingRun -DauthVu=50 -DunauthVu=50
 * - URL 변경: ./gradlew gatlingRun -DbaseUrl=http://prod-server:8080
 * - 세션 시간 변경: ./gradlew gatlingRun -DauthMinDuration=300 -DauthMaxDuration=900
 */
public class Config {

    private Config() {
    }

    // ==================== 테스트 대상 설정 ====================

    /**
     * HTTP 기본 URL
     */
    public static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    /**
     * WebSocket 기본 URL
     */
    public static final String WS_BASE_URL = System.getProperty("wsBaseUrl", "ws://localhost:8080");

    // ==================== 가상 사용자(VU) 설정 ====================

    /**
     * 인증 사용자 수 (기본값: 5)
     */
    public static final int AUTH_VU = Integer.getInteger("authVu", 5);

    /**
     * 비인증 사용자 수 (기본값: 5)
     */
    public static final int UNAUTH_VU = Integer.getInteger("unauthVu", 5);

    /**
     * DB에 적재된 최대 테스트 사용자 수 (기본값: 10)
     * 소규모 테스트 시: -DmaxUsers=10
     * 대규모 테스트 시: -DmaxUsers=10000
     */
    public static final int MAX_USERS = Integer.getInteger("maxUsers", 10);

    // ==================== 세션 유지 시간 설정 (초 단위) ====================

    /**
     * 인증 사용자 최소 세션 유지 시간 (기본값: 60초 = 1분)
     */
    public static final int AUTH_MIN_DURATION = Integer.getInteger("authMinDuration", 60);

    /**
     * 인증 사용자 최대 세션 유지 시간 (기본값: 120초 = 2분)
     */
    public static final int AUTH_MAX_DURATION = Integer.getInteger("authMaxDuration", 120);

    /**
     * 비인증 사용자 최소 세션 유지 시간 (기본값: 30초)
     */
    public static final int UNAUTH_MIN_DURATION = Integer.getInteger("unauthMinDuration", 30);

    /**
     * 비인증 사용자 최대 세션 유지 시간 (기본값: 150초 = 2.5분)
     */
    public static final int UNAUTH_MAX_DURATION = Integer.getInteger("unauthMaxDuration", 150);

    // ==================== 테스트 대상 리소스 ====================

    /**
     * 테스트할 라이브 스트리밍 ID (기본값: 1)
     */
    public static final Long LIVESTREAM_ID = Long.getLong("livestreamId", 1L);
}