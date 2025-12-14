package performance.utils;

import io.gatling.javaapi.core.OpenInjectionStep;

import static io.gatling.javaapi.core.CoreDsl.*;

/**
 * Gatling 부하 주입 프로파일 관리
 * <p>
 * 다양한 성능 테스트 시나리오에 맞는 부하 주입 패턴을 제공합니다.
 * <p>
 * 사용 예시:
 * <pre>
 * setUp(
 *     scenario.injectOpen(LoadProfiles.spike(100)),
 *     scenario.injectOpen(LoadProfiles.rampUp(100, 60))
 * );
 * </pre>
 */
public class LoadProfiles {

    private LoadProfiles() {
    }

    // ==================== 스파이크 테스트 ====================

    /**
     * 스파이크 테스트: 모든 사용자를 동시에 시작
     * <p>
     * 용도: 갑작스러운 트래픽 급증 시나리오 (이벤트 오픈, 인기 방송 시작 등)
     * <p>
     * 시각화:
     * <pre>
     * 사용자 수
     * 100 ████████
     *  50 ████████
     *   0 ────────
     *     0초 →
     * </pre>
     *
     * @param users 동시에 시작할 사용자 수
     * @return 부하 주입 스텝
     */
    public static OpenInjectionStep spike(final int users) {
        return atOnceUsers(users);
    }

    // ==================== 램프 업 테스트 ====================

    /**
     * 램프 업 테스트: 지정된 시간 동안 사용자를 점진적으로 증가
     * <p>
     * 용도: 일반적인 트래픽 증가 패턴 (서비스 워밍업, 점진적 유입)
     * <p>
     * 시각화:
     * <pre>
     * 사용자 수
     * 100         ████
     *  50     ████
     *   0 ────
     *     0초 → 60초
     * </pre>
     *
     * @param users    목표 사용자 수
     * @param duration 램프 업 지속 시간 (초)
     * @return 부하 주입 스텝
     */
    public static OpenInjectionStep rampUp(final int users, final int duration) {
        return rampUsers(users).during(duration);
    }

    // ==================== 일정 속도 테스트 ====================

    /**
     * 일정 속도 테스트: 초당 일정한 속도로 사용자 추가
     * <p>
     * 용도: 안정적인 트래픽 유입 패턴 (일정한 신규 접속)
     * <p>
     * 시각화:
     * <pre>
     * 사용자 수
     * 600     ／
     * 300   ／
     *   0 ／
     *     0초 → 60초
     *     (초당 10명 추가)
     * </pre>
     *
     * @param usersPerSec 초당 추가할 사용자 수
     * @param duration    지속 시간 (초)
     * @return 부하 주입 스텝
     */
    public static OpenInjectionStep constantRate(final double usersPerSec, final int duration) {
        return constantUsersPerSec(usersPerSec).during(duration);
    }

    // ==================== 단계적 증가 테스트 ====================

    /**
     * 단계적 증가 테스트: 초당 사용자 수를 점진적으로 증가
     * <p>
     * 용도: 시스템 한계점 탐색 (어느 지점에서 성능 저하가 발생하는지 확인)
     * <p>
     * 시각화:
     * <pre>
     * 사용자 수
     * 3000           ／
     * 1500       ／
     *    0 ──／
     *     0초 → 60초
     *     (초당 1명 → 초당 50명까지 증가)
     * </pre>
     *
     * @param startRate 시작 속도 (초당 사용자 수)
     * @param endRate   종료 속도 (초당 사용자 수)
     * @param duration  지속 시간 (초)
     * @return 부하 주입 스텝
     */
    public static OpenInjectionStep incrementalRate(final double startRate, final double endRate, final int duration) {
        return rampUsersPerSec(startRate)
                .to(endRate)
                .during(duration);
    }

    // ==================== 복합 패턴 ====================

    /**
     * 스트레스 피크 테스트: 점진적 증가 후 피크 유지 후 감소
     * <p>
     * 용도: 실제 트래픽 패턴 시뮬레이션 (증가 → 피크 → 감소)
     * <p>
     * 시각화:
     * <pre>
     * 사용자 수
     * 100     ／￣￣＼
     *  50   ／        ＼
     *   0 ／            ＼
     *     램프업  피크  램프다운
     * </pre>
     *
     * @param peakUsers       피크 사용자 수
     * @param rampUpDuration  램프 업 시간 (초)
     * @param peakDuration    피크 유지 시간 (초)
     * @param rampDownDuration 램프 다운 시간 (초)
     * @return 부하 주입 스텝 배열
     */
    public static OpenInjectionStep[] stressPeak(
            final int peakUsers,
            final int rampUpDuration,
            final int peakDuration,
            final int rampDownDuration
    ) {
        return new OpenInjectionStep[]{
                rampUsers(peakUsers).during(rampUpDuration),
                constantUsersPerSec(0).during(peakDuration),
                rampUsers(0).during(rampDownDuration)
        };
    }
}