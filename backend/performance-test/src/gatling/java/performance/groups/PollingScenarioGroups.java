package performance.groups;

import io.gatling.javaapi.core.ChainBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static performance.endpoints.LiveStreamingV2Endpoints.*;
import static performance.utils.ChatTestDataFeeder.createInitialChatMessageFeeder;
import static performance.utils.ChatTestDataFeeder.createNormalChatMessageFeeder;

/**
 * 폴링 방식 라이브 스트리밍 테스트 시나리오 그룹
 * <p>
 * WebSocket 대신 HTTP 폴링을 사용하여 실시간 데이터를 조회하는 시나리오를 제공합니다.
 * Gatling의 group() API를 활용하여 논리적 단위로 그룹화하여 리포트에서 각 그룹별 통계를 확인할 수 있습니다.
 */
public class PollingScenarioGroups {

    private PollingScenarioGroups() {
    }

    /**
     * 라이브 스트리밍 메타데이터 조회 그룹
     * <p>
     * 인증/비인증 사용자 모두가 라이브 스트리밍에 접속하기 전에
     * 메타데이터(제목, 스트리머 정보 등)를 조회합니다.
     */
    public static final ChainBuilder fetchMetadata =
            group("메타데이터 조회 (폴링)").on(
                    getLiveStreamMetadata
            );

    /**
     * 초기 채팅 조회 그룹
     * <p>
     * 라이브 스트리밍 접속 시 최초 채팅 목록을 가져옵니다.
     * lastChatId를 세션에 저장하여 이후 폴링에 사용합니다.
     */
    public static final ChainBuilder fetchInitialChats =
            group("초기 채팅 조회").on(
                    exec(getInitialChats)
            );

    /**
     * 좋아요/싫어요 반응 그룹 (선택적)
     * <p>
     * 인증 사용자 중 10%만 라이브 스트리밍을 시청하면서 좋아요 또는 싫어요를 선택합니다.
     * 실제 사용자 행동 패턴을 반영하여 반응하는 사용자 중 대부분(95%)은 좋아요를 선택합니다.
     * <p>
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
                            group("좋아요/싫어요 반응 (폴링)").on(
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

    /**
     * 공통 폴링 로직
     * <p>
     * 인증/비인증 사용자 모두 사용하는 폴링 행동입니다.
     * <p>
     * 실행 흐름:
     * - pollCycle 증가
     * - 실시간 통계 폴링 (20초마다 = 5번에 한 번)
     * - 새 채팅 폴링 (4초마다 = 매번)
     * - 4초 대기
     */
    public static final ChainBuilder pollingBehavior =
            exec(session -> session.set("pollCycle", session.getInt("pollCycle") + 1))
            // 실시간 통계 폴링 (20초마다 = 5번에 한 번)
            .doIf(session -> session.getInt("pollCycle") % 5 == 0).then(
                    exec(pollLiveStats)
            )
            // 채팅 폴링 (매번)
            .exec(pollNewChats)
            .pause(4);

    /**
     * 인증 사용자의 전체 세션 동안의 행동
     * <p>
     * 채팅 전송과 폴링을 분리하여 실제 사용자 행동 패턴을 시뮬레이션합니다.
     * <p>
     * 실행 흐름:
     * - 초기 5분: 10% 확률로 채팅 전송 (평균 40초에 한 번)
     * - 이후: 4% 확률로 채팅 전송 (평균 100초에 한 번)
     * - 실시간 통계 폴링 (20초마다 = 5번에 한 번)
     * - 새 채팅 폴링 (4초마다 = 매번)
     */
    public static final ChainBuilder authenticatedUserPollingBehavior =
            group("인증 사용자 행동 (채팅 + 폴링)").on(
                    exec(session -> session.set("pollCycle", 0))
                    // 초기 5분 (10% 확률)
                    .during(session -> Duration.ofSeconds(Math.min(300, session.getInt("sessionDuration")))).on(
                            // 채팅 전송 (10% 확률)
                            randomSwitch().on(
                                    percent(10.0).then(
                                            feed(createInitialChatMessageFeeder())
                                                    .exec(sendChatMessage)
                                    )
                            )
                            .exec(pollingBehavior)
                    )
                    // 나머지 시간 (4% 확률)
                    .during(session -> Duration.ofSeconds(Math.max(0, session.getInt("sessionDuration") - 300))).on(
                            // 채팅 전송 (4% 확률)
                            randomSwitch().on(
                                    percent(4.0).then(
                                            feed(createNormalChatMessageFeeder())
                                                    .exec(sendChatMessage)
                                    )
                            )
                            .exec(pollingBehavior)
                    )
            );

    /**
     * 비인증 사용자의 전체 세션 동안의 폴링 행동
     * <p>
     * 채팅 전송 없이 폴링만 수행합니다.
     * <p>
     * 실행 흐름:
     * - sessionDuration 동안 4초마다 반복:
     *   1. 실시간 통계 폴링 (20초마다 = 5번에 한 번)
     *   2. 새 채팅 폴링 (4초마다 = 매번)
     */
    public static final ChainBuilder unauthenticatedUserPollingBehavior =
            group("비인증 사용자 행동 (폴링만)").on(
                    exec(session -> session.set("pollCycle", 0))
                    .during("#{sessionDuration}").on(
                            exec(pollingBehavior)
                    )
            );
}
