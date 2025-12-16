package performance.groups;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.percent;
import static io.gatling.javaapi.core.CoreDsl.randomSwitch;
import static performance.endpoints.LiveStreamingV1Endpoints.*;

/**
 * 라이브 스트리밍 관련 시나리오 그룹
 *
 * Gatling의 group() API를 활용하여 라이브 스트리밍 관련 동작을 논리적 단위로 그룹화합니다.
 * 이를 통해 Gatling 리포트에서 각 그룹별 통계를 확인할 수 있습니다.
 *
 * 각 그룹은 독립적인 ChainBuilder로 구성되어 재사용 및 재조합이 가능합니다.
 */
public class LiveStreamingScenarioGroups {

    private LiveStreamingScenarioGroups() {
    }

    /**
     * 라이브 스트리밍 메타데이터 조회 그룹
     *
     * 인증/비인증 사용자 모두가 라이브 스트리밍에 접속하기 전에
     * 메타데이터(제목, 스트리머 정보 등)를 조회합니다.
     */
    public static final ChainBuilder fetchMetadata =
            group("메타데이터 조회").on(
                    getLiveStreamMetadata
            );

    /**
     * 좋아요/싫어요 반응 그룹 (선택적)
     *
     * 인증 사용자 중 10%만 라이브 스트리밍을 시청하면서 좋아요 또는 싫어요를 선택합니다.
     * 실제 사용자 행동 패턴을 반영하여 반응하는 사용자 중 대부분(95%)은 좋아요를 선택합니다.
     *
     * 실행 흐름:
     * - 10% 확률: 반응 선택
     *   - 그 중 95% 확률: 좋아요 선택
     *   - 그 중 5% 확률: 싫어요 선택
     * - 90% 확률: 반응하지 않음
     */
    public static final ChainBuilder reactToStream =
            randomSwitch().on(
                    // 10% 확률로 반응 선택
                    percent(10.0).then(
                            group("좋아요/싫어요 반응").on(
                                    randomSwitch().on(
                                            // 95% 확률로 좋아요 선택
                                            percent(95.0).then(toggleLike),
                                            // 5% 확률로 싫어요 선택
                                            percent(5.0).then(toggleDislike)
                                    )
                            )
                    )
                    // 90% 확률로 반응하지 않음 (아무것도 하지 않음)
            );
}
