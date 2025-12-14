package performance.utils;

import io.gatling.javaapi.core.FeederBuilder;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.listFeeder;

/**
 * 테스트 데이터 Feeder 생성 유틸리티
 * <p>
 * 성능 테스트에 필요한 사용자 데이터를 생성합니다.
 */
public class TestDataFeeder {

    /**
     * 사용자 Feeder 생성
     * <p>
     * 전제: DB에 loadtest1@test.com ~ loadtest{maxUsers}@test.com 사용자가 존재
     * 모든 사용자의 비밀번호: password123
     *
     * @param maxUsers DB에 적재된 최대 테스트 사용자 수
     * @return circular 전략으로 순환하는 사용자 Feeder (VU 수가 maxUsers를 초과하면 처음부터 재사용)
     */
    public static FeederBuilder<Object> createUserFeeder(final int maxUsers) {
        return listFeeder(
                IntStream.rangeClosed(1, maxUsers)
                        .mapToObj(i -> Map.<String, Object>of(
                                "email", "loadtest" + i + "@test.com",
                                "password", "password123"
                        ))
                        .collect(Collectors.toList())
        ).circular();
    }

    /**
     * 사용자 행동 패턴 Feeder 생성
     * <p>
     * 각 가상 사용자(VU)마다 다른 행동 패턴을 랜덤하게 생성합니다.
     * 실제 시나리오: 사용자마다 라이브 스트리밍 시청 시간이 다름
     * <p>
     * 향후 확장 가능: 채팅 전송 빈도, 좋아요 누르는 빈도 등 추가 가능
     * <p>
     * ThreadLocalRandom 사용: Gatling의 멀티스레드 환경에서 동시성 경합 없이 랜덤 값 생성
     *
     * @param minSessionDuration 최소 세션 시간 (초)
     * @param maxSessionDuration 최대 세션 시간 (초)
     * @return 무한 Iterator (sessionDuration 키로 제공, .feed()에 직접 사용 가능)
     */
    public static Iterator<Map<String, Object>> createBehaviorFeeder(
            final int minSessionDuration,
            final int maxSessionDuration
    ) {
        return Stream.generate(() -> {
            final int sessionDuration = ThreadLocalRandom.current()
                    .nextInt(minSessionDuration, maxSessionDuration + 1);
            return Map.<String, Object>of("sessionDuration", sessionDuration);
        }).iterator();
    }

    private TestDataFeeder() {
        // 유틸리티 클래스: 인스턴스화 방지
    }
}