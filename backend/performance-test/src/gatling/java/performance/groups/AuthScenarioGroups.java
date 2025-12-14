package performance.groups;

import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.group;
import static performance.endpoints.AuthEndpoints.login;

/**
 * 인증 관련 시나리오 그룹
 *
 * Gatling의 group() API를 활용하여 인증 관련 동작을 논리적 단위로 그룹화합니다.
 * 이를 통해 Gatling 리포트에서 각 그룹별 통계를 확인할 수 있습니다.
 *
 * 각 그룹은 독립적인 ChainBuilder로 구성되어 재사용 및 재조합이 가능합니다.
 */
public class AuthScenarioGroups {

    private AuthScenarioGroups() {
    }

    /**
     * 인증 그룹
     *
     * 로그인 API를 호출하여 JSESSIONID 쿠키를 획득합니다.
     */
    public static final ChainBuilder authenticate =
            group("인증").on(
                    login
            );
}
