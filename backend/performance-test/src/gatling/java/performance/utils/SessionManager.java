package performance.utils;

import io.gatling.javaapi.core.FeederBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static io.gatling.javaapi.core.CoreDsl.csv;

/**
 * 세션 관리 유틸리티
 * <p>
 * Setup Simulation에서 생성한 세션 정보를 CSV 파일로 저장하고,
 * 실제 성능 테스트에서 해당 세션을 로드하여 사용합니다.
 * <p>
 * 파일 형식: CSV (덮어쓰기 방식)
 * 파일 위치: backend/performance-test/src/gatling/java/performance/simulation/session/sessions.csv
 * 파일 구조: userId,email,username,sessionId
 */
public class SessionManager {

    /**
     * 세션 파일 경로 (고정, 덮어쓰기 방식)
     * <p>
     * 상대 경로: src/gatling/java/performance/simulation/session/sessions.csv
     * 절대 경로: backend/performance-test/src/gatling/java/performance/simulation/session/sessions.csv
     */
    private static final String SESSION_FILE_PATH = "src/gatling/java/performance/simulation/session/sessions.csv";

    private SessionManager() {
        // 유틸리티 클래스: 인스턴스화 방지
    }

    /**
     * 세션 파일 초기화 (덮어쓰기)
     * <p>
     * 기존 파일을 삭제하고 CSV 헤더를 작성합니다.
     * SessionSetupSimulation 시작 시 호출됩니다.
     */
    public static void initializeSessionFile() {
        final Path path = Paths.get(SESSION_FILE_PATH);

        try {
            // sessions 디렉토리 생성 (존재하지 않으면)
            Files.createDirectories(path.getParent());

            // 기존 파일 삭제 (존재하면)
            Files.deleteIfExists(path);

            // 새 파일 생성 및 헤더 작성
            Files.writeString(
                    path,
                    "userId,email,username,sessionId\n",
                    StandardOpenOption.CREATE
            );

            System.out.println("세션 파일 초기화 완료: " + path.toAbsolutePath());
        } catch (final IOException e) {
            throw new RuntimeException("세션 파일 초기화 실패: " + SESSION_FILE_PATH, e);
        }
    }

    /**
     * 세션 정보를 파일에 저장 (append 모드)
     * <p>
     * Thread-safe: synchronized를 사용하여 동시성 문제 방지
     *
     * @param userId    사용자 ID
     * @param email     이메일
     * @param username  사용자명
     * @param sessionId JSESSIONID 쿠키 값
     */
    public static synchronized void saveSession(
            final String userId,
            final String email,
            final String username,
            final String sessionId
    ) {
        try (final BufferedWriter writer = new BufferedWriter(
                new FileWriter(SESSION_FILE_PATH, true))) {
            // CSV 행 작성: userId,email,username,sessionId
            writer.write(String.format("%s,%s,%s,%s%n", userId, email, username, sessionId));
        } catch (final IOException e) {
            System.err.println("세션 저장 실패 - userId: " + userId + ", email: " + email);
            throw new RuntimeException("세션 저장 실패", e);
        }
    }

    /**
     * 저장된 세션 파일을 Gatling Feeder로 로드
     * <p>
     * LiveStreamingLoadTestSimulation에서 사용합니다.
     * circular() 전략으로 VU 수가 세션 수를 초과하면 처음부터 재사용합니다.
     *
     * @return 세션 Feeder (circular 전략)
     * @throws RuntimeException 세션 파일이 존재하지 않거나 읽기 실패 시
     */
    public static FeederBuilder<String> loadSessionFeeder() {
        final Path path = Paths.get(SESSION_FILE_PATH);

        if (!Files.exists(path)) {
            throw new RuntimeException(
                    "세션 파일이 존재하지 않습니다: " + path.toAbsolutePath() + "\n" +
                            "먼저 SessionSetupSimulation을 실행하여 세션을 생성하세요."
            );
        }

        try {
            final long lineCount = Files.lines(path).count() - 1; // 헤더 제외
            System.out.println("로드된 세션 수: " + lineCount);
        } catch (final IOException e) {
            System.err.println("세션 파일 읽기 실패: " + SESSION_FILE_PATH);
        }

        return csv(SESSION_FILE_PATH).circular();
    }

    /**
     * 세션 파일 경로 반환 (디버깅용)
     *
     * @return 세션 파일의 절대 경로
     */
    public static String getSessionFilePath() {
        return Paths.get(SESSION_FILE_PATH).toAbsolutePath().toString();
    }
}