# 멀티모듈 환경에서 WebSocket E2E 테스트 구성

## 배경

실시간 채팅 기능 개발 중, `live-streaming:interaction` 모듈에서 WebSocket E2E 테스트를 작성할 때 문제가 발생했습니다.

### 프로젝트 구조

```
[프로덕션 의존성]
api (CloneApplication) → live-streaming:interaction → core

- api: 애플리케이션 진입점 (@SpringBootApplication), 인증 API (AuthController, AuthService)
- interaction: WebSocket 채팅 기능 (WebSocketChatController, LiveStreaming 엔티티)
- core: 공통 도메인 (User, Channel 엔티티)
```

현재 애플리케이션 실행은 `api` 모듈의 `CloneApplication`을 통해 이루어집니다.

### 문제 상황

WebSocket E2E 테스트에서는 다음이 필요합니다:

1. **전체 애플리케이션 컨텍스트**: HTTP 세션 기반 인증, WebSocket 연결, 메시지 라우팅 등 실제 환경과 동일한 통합 테스트
2. **인증 API**: 로그인(`AuthController`) → HTTP 세션 생성 → WebSocket 연결 시 세션 복사
3. **모든 엔티티/컴포넌트 스캔**: `core`의 `User`, `Channel` + `interaction`의 `LiveStreaming` + `api`의 `AuthService`

하지만 `interaction` 모듈에서 단순히 테스트를 작성하면:

```java
@SpringBootTest
class WebSocketChatControllerTest {
    @Autowired
    private UserRepository userRepository;  // ❌ Bean을 찾을 수 없음

    @Autowired
    private AuthController authController;  // ❌ Bean을 찾을 수 없음
}
```

**원인**: `interaction` 모듈은 독립 실행 모듈이 아니며, `api` 모듈에 의존되는 하위 모듈입니다. 자체적으로 `@SpringBootApplication`이 없어 테스트 컨텍스트 구성이 불가능합니다.

## 기술적 의사결정

### 테스트 전략 고민: Mock vs 실제 통합

WebSocket E2E 테스트를 구성하는 방법은 크게 두 가지가 있습니다:

#### 방안 1: Mock 사용

```java
@WebMvcTest(WebSocketChatController.class)
class WebSocketChatControllerTest {
    @MockBean
    private AuthService authService;

    @MockBean
    private LiveStreamingChatService liveStreamingChatService;

    @Test
    void sendMessage() {
        // given
        given(authService.login(any())).willReturn(new LoginResponse("token"));
        given(liveStreamingChatService.sendMessage(any(), any(), any(), any()))
            .willReturn(new LiveStreamingChatInfo(...));

        // when & then
        // WebSocket 메시지 전송 테스트
    }
}
```

**장점**:
- 의존성 역전 불필요
- 테스트 속도 빠름
- 통합 테스트 수준 격리

**단점**:
- ⚠️ **구현 세부사항에 의존**: Mock 설정을 위해 서비스 레이어의 내부 동작을 알아야 함
- ⚠️ **유지보수성 저하**: 서비스 로직이 변경될 때마다 Mock 설정도 함께 수정 필요
- ⚠️ **통합 검증 불가**: HTTP 세션 → WebSocket 세션 복사, STOMP 라우팅, 브로드캐스트 등 실제 통합 동작을 검증할 수 없음

#### 방안 2: 실제 E2E 테스트

실제 사용자 시나리오를 그대로 검증:

```
[E2E 테스트 시나리오]
1. HTTP POST /auth/login → JSESSIONID 발급 (api 모듈의 AuthController)
2. WebSocket 연결 (/ws) → HTTP 세션 복사로 사용자 인증 (HttpSessionHandshakeInterceptor)
3. STOMP 메시지 전송 (/app/chat/rooms/{roomId}/messages)
4. 구독자들이 메시지 수신 확인 (/topic/room/{roomId})
```

**필요한 것**:
- `api` 모듈의 인증 컨트롤러 및 서비스
- `core` 모듈의 User, Channel 엔티티
- `interaction` 모듈의 WebSocket 설정 및 채팅 컨트롤러

**장점**:
- ✅ 실제 통합 동작 검증
- ✅ 유지보수 용이 (구현 세부사항 변경 시 테스트 코드 수정 불필요)
- ✅ 높은 신뢰도

**단점**:
- 테스트 의존성 역전 필요
- 테스트 시간 증가

### 선택: E2E 테스트 + 테스트 의존성 역전

**이유**:
1. **유지보수성 우선**: 서비스 로직이 변경되어도 테스트 코드 수정 불필요
2. **실제 통합 검증**: WebSocket 전체 플로우를 실제 환경과 동일하게 검증
3. **비용 대비 효과**: JAR 파일 하나 추가되는 오버헤드는 미미하지만, 얻는 테스트 신뢰도는 높음

**결론**: 현재는 **모놀리틱 멀티모듈** 프로젝트이므로 Mock이 아닌 실제 통합 테스트를 선택. MSA로 전환 시 외부 서비스 호출 부분만 Mock 처리 고려.

### 의존성 방향

```
[프로덕션]
api → interaction → core  (정방향)

[테스트]
interaction:test → api (역방향, 테스트에서만)
```

프로덕션에서는 단방향 의존성을 유지하고, 테스트에서만 `interaction` 모듈이 `api` 모듈을 참조하도록 구성합니다.

## 문제: Spring Boot의 bootJar 제약

### 테스트 실패

`jar.enabled = false`로 설정하면:

```kotlin
// api/build.gradle.kts (기본 상태)
tasks.named<Jar>("jar") {
    enabled = false  // ❌ 테스트 실패 유발
}
```

**결과**: 빌드는 성공하지만, 테스트 실행 시 classpath에서 `api` 모듈의 클래스를 찾지 못해 런타임 오류 발생

```
WebSocketChatControllerTest FAILED
    Caused by: com.fasterxml.jackson.databind.exc.MismatchedInputException
```

### 원인: jar.enabled = false 설정

기존 프로젝트에서는 일반 아카이브(plain jar)가 불필요하다고 판단하여 `jar` 태스크를 명시적으로 비활성화했습니다:

```kotlin
// api/build.gradle.kts (기존 설정)
tasks.named<BootJar>("bootJar") {
    enabled = true
    mainClass.set("com.youtube.api.CloneApplication")
}

tasks.named<Jar>("jar") {
    enabled = false  // ❌ 일반 jar 생성을 막기 위해 명시적으로 비활성화
}
```

**Spring Boot Gradle Plugin의 기본 동작**:
- `bootJar`와 `jar` 태스크가 **모두 활성화**
- `bootJar` → `application.jar` (실행 가능한 fat jar, 모든 의존성 포함)
- `jar` → `application-plain.jar` (일반 라이브러리 jar, 다른 모듈이 의존성으로 사용 가능)

[Spring Boot Gradle Plugin 공식 문서](https://docs.spring.io/spring-boot/gradle-plugin/packaging.html)

`api` 모듈은 실행 가능한 애플리케이션이므로 배포 시 일반 jar가 불필요하다고 판단하여 `jar.enabled = false`로 설정했습니다.

**이로 인한 결과**:
- ✅ `bootJar`: 실행 가능한 fat jar (모든 의존성 포함) → 배포용
- ❌ `jar` (`-plain.jar`): `enabled = false`로 인해 **생성되지 않음** → 다른 모듈의 테스트에서 의존성으로 참조 불가

`jar.enabled = false`로 설정하면 Gradle이 다른 모듈의 테스트 클래스패스에 `api` 모듈의 클래스를 포함시킬 수 있는 일반 jar 파일이 생성되지 않아, `interaction` 모듈의 테스트 실행 시 `api` 모듈의 컴포넌트를 찾을 수 없습니다.

## 해결 방안 검토

E2E 테스트를 선택한 후, 구체적인 구현 방법을 검토했습니다:

| 방안 | 장점 | 단점 | 선택 |
|------|------|------|------|
| **api 모듈의 jar 활성화 + InteractionTestApplication** | 실제 통합 검증, 유지보수 용이, 테스트 격리 | 테스트용 Application 클래스 필요, plain jar 추가 생성 | ✅ |
| api 모듈의 jar 활성화 + CloneApplication 직접 사용 | 설정 재사용, 별도 Application 불필요 | api 모듈 전체를 테스트 의존성으로 추가 (역의존 증가) | ❌ |

## 해결 과정

### 1. api 모듈의 jar 태스크 활성화

```kotlin
// api/build.gradle.kts
tasks.named<Jar>("jar") {
    enabled = true  // ✅ 일반 jar 생성 활성화
}
```

**효과**: `api-plain.jar`가 생성되어 다른 모듈의 테스트에서 `api` 모듈의 클래스를 참조할 수 있게 됩니다.

### 2. InteractionTestApplication 생성

테스트 소스셋에 독립적인 `@SpringBootApplication` 생성:

```java
// interaction/src/test/java/.../InteractionTestApplication.java
@SpringBootApplication
@EntityScan(basePackages = {
    "com.youtube.api",
    "com.youtube.core",     // User, Channel 엔티티
    "com.youtube.live.interaction"  // LiveStreaming, LiveStreamingChat 엔티티
})
@ComponentScan(basePackages = {
    "com.youtube.api",      // AuthController, AuthService
    "com.youtube.core",     // UserReader, UserWriter
    "com.youtube.live.interaction"  // WebSocketChatController, LiveStreamingChatService
})
@EnableJpaRepositories(basePackages = {
    "com.youtube.api",
    "com.youtube.core",
    "com.youtube.live.interaction"
})
@EnableJpaAuditing
public class InteractionTestApplication {
    public static void main(final String[] args) {
        SpringApplication.run(InteractionTestApplication.class, args);
    }
}
```

**역할**:
- 테스트 실행 시 `api`, `core`, `interaction` 모듈의 모든 컴포넌트를 스캔
- 필요한 모든 엔티티, 리포지토리, 서비스를 Spring 컨텍스트에 등록하여 완전한 통합 테스트 환경 구성
- `api-plain.jar`의 클래스들을 테스트 클래스패스에서 사용 가능하게 함

### 3. JPA 설정 충돌 방지

각 모듈의 개별 JPA 설정이 테스트에서 중복 적용되지 않도록 프로필 설정:

```java
// core/src/main/java/com/youtube/core/config/JPAConfig.java
@Configuration
@Profile("!websocket-test")  // 테스트에서는 비활성화
@EntityScan(basePackages = "com.youtube.core")
@EnableJpaRepositories(basePackages = "com.youtube.core")
@EnableJpaAuditing
public class JPAConfig { }
```

```java
// interaction/src/main/java/.../config/LiveStreamingJPAConfig.java
@Configuration
@Profile("!websocket-test")  // 테스트에서는 비활성화
@EntityScan(basePackages = "com.youtube.live.interaction")
@EnableJpaRepositories(basePackages = "com.youtube.live.interaction")
public class LiveStreamingJPAConfig { }
```

**이유**: `InteractionTestApplication`이 이미 모든 패키지를 스캔하므로, 개별 모듈의 JPA 설정은 중복입니다.

### 4. 테스트 베이스 클래스 구성

```java
@SpringBootTest(
    classes = InteractionTestApplication.class,
    webEnvironment = RANDOM_PORT
)
@ActiveProfiles("websocket-test")
public abstract class WebSocketStompTest extends TestContainer {
    @LocalServerPort
    protected int port;

    @Autowired
    protected TestPersistSupport testSupport;

    protected String wsUrl;

    @BeforeEach
    public void setUpWebSocket() {
        wsUrl = String.format("ws://localhost:%d/ws", port);
    }
}
```

### 5. 테스트 의존성 구성

```kotlin
// live-streaming/interaction/build.gradle.kts
dependencies {
    implementation(project(":core"))

    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":api")))  // TestAuthSupport 등 테스트 헬퍼
}
```

**주요 포인트**:
- `api` 모듈의 **프로덕션 코드**(컨트롤러, 서비스)는 `api-plain.jar`를 통해 테스트 클래스패스에 자동 포함되며, `InteractionTestApplication`이 컴포넌트 스캔으로 등록
- `testFixtures`만 명시적으로 의존성에 추가하여 테스트 헬퍼(`TestAuthSupport`) 사용
- 테스트 역방향 의존성을 `testFixtures`로 최소화

## 결과

### 빌드 산출물

```
api/build/libs/
  ├─ api-0.0.1-SNAPSHOT.jar              // 배포용 bootJar (fat jar)
  ├─ api-0.0.1-SNAPSHOT-plain.jar        // 라이브러리용 일반 jar (interaction 테스트가 참조)
  └─ api-0.0.1-SNAPSHOT-test-fixtures.jar // 테스트 헬퍼 (TestAuthSupport)
```

### 의존성 방향

```
[프로덕션]
api → interaction → core  (단방향 유지)

[테스트 컴파일타임]
interaction:test → api:testFixtures (testFixtures만 명시적 의존)

[테스트 런타임]
interaction:test → api-plain.jar (Gradle이 자동 해결)
                 ↓
         InteractionTestApplication이 api 프로덕션 코드 컴포넌트 스캔
```

### 아키텍처 영향

- ✅ **프로덕션 의존성**: 단방향 유지 (`api` → `interaction` → `core`)
- ✅ **테스트 격리**: 각 모듈이 독립적인 테스트 애플리케이션 구성 가능
- ✅ **역의존 최소화**: `testFixtures`만 명시적으로 의존, 프로덕션 코드는 런타임에만 참조
- ✅ **빌드 산출물**: `-plain.jar` 추가 생성되지만 배포 대상이 아니며 용량 오버헤드 미미

### 검증 가능한 시나리오

E2E 테스트로 다음을 검증할 수 있게 되었습니다:

- ✅ HTTP 로그인 API를 통한 세션 생성 (`AuthController`)
- ✅ WebSocket 연결 시 HTTP 세션 자동 복사 (`HttpSessionHandshakeInterceptor`)
- ✅ STOMP 메시지 전송 및 라우팅
- ✅ 실시간 브로드캐스트 (`SimpleBroker`)
- ✅ 다중 클라이언트 동시 접속 및 메시지 수신
- ✅ 메시지 순서 보장 (`preservePublishOrder`)
- ✅ 에러 핸들링 (`@MessageExceptionHandler` + `@SendToUser`)

