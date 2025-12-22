# Native Headers vs Simp Headers 구조

> 본 문서는 Spring Framework 공식 레퍼런스를 기반으로 작성되었습니다.

## 1. 개요

Spring Framework의 STOMP 메시징에서는 두 가지 형태의 헤더를 관리합니다:

- **Native Headers**: 원시 STOMP 프로토콜 헤더
- **SIMP Headers**: 프레임워크가 정규화한 공통 처리 헤더

## 2. 클래스 계층 구조

```
MessageHeaderAccessor
    │
    └── NativeMessageHeaderAccessor
            │
            └── SimpMessageHeaderAccessor
                    │
                    └── StompHeaderAccessor
```

### 각 클래스의 역할

| 클래스 | 역할 |
|--------|------|
| `MessageHeaderAccessor` | 메시지 헤더 접근을 위한 기본 추상화 |
| `NativeMessageHeaderAccessor` | **Native headers를 `Map<String, List<String>>` 형태로 저장** |
| `SimpMessageHeaderAccessor` | **SIMP headers를 타입 안전한 속성으로 관리** |
| `StompHeaderAccessor` | STOMP 프레임과 Spring Message 간 변환 처리 |

## 3. 헤더 저장 구조

### 3.1 전체 구조 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                     Spring Message                          │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │              Message Headers (Map)                    │ │
│  │                                                       │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │   SIMP Headers                                  │ │ │
│  │  │   (SimpMessageHeaderAccessor가 관리)            │ │ │
│  │  │                                                 │ │ │
│  │  │   - DESTINATION_HEADER: "/topic/greeting"      │ │ │
│  │  │   - SESSION_ID_HEADER: "abc123"                │ │ │
│  │  │   - SUBSCRIPTION_ID_HEADER: "sub-0"            │ │ │
│  │  │   - CONTENT_TYPE_HEADER: "application/json"    │ │ │
│  │  │   - USER_HEADER: User principal                │ │ │
│  │  │   - MESSAGE_TYPE_HEADER: MESSAGE               │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  │                                                       │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │   "nativeHeaders" 키                            │ │ │
│  │  │   (NativeMessageHeaderAccessor가 관리)          │ │ │
│  │  │                                                 │ │ │
│  │  │   Map<String, List<String>>:                   │ │ │
│  │  │   {                                             │ │ │
│  │  │     "destination": ["/topic/greeting"],        │ │ │
│  │  │     "content-type": ["application/json"],      │ │ │
│  │  │     "message-id": ["msg-001"],                 │ │ │
│  │  │     "subscription": ["sub-0"],                 │ │ │
│  │  │     "ack": ["auto"],                           │ │ │
│  │  │     "custom-header": ["value1", "value2"]      │ │ │
│  │  │   }                                             │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 공식 레퍼런스 설명

Spring Framework 공식 문서에 따르면:

> "When created from STOMP frame content, the actual STOMP headers are stored in the native header sub-map managed by the parent class `NativeMessageHeaderAccessor` while the parent class `SimpMessageHeaderAccessor` manages common processing headers some of which are based on STOMP headers (for example, destination, content-type, etc)."

**핵심 포인트:**
- STOMP 프레임의 실제 헤더들은 **Native header sub-map**에 저장됨
- 공통 처리 헤더들은 **SimpMessageHeaderAccessor**가 별도로 관리
- 일부 SIMP 헤더는 STOMP 헤더를 기반으로 생성됨 (예: destination, content-type)

### 3.3 Message 인터페이스와 MessageHeaders 구조

#### Message<T> 인터페이스

Spring의 `Message<T>`는 헤더와 페이로드를 포함하는 제네릭 메시지 표현입니다.

```java
public interface Message<T> {
    T getPayload();              // 메시지 본문 반환
    MessageHeaders getHeaders(); // 메시지 헤더 반환 (never null)
}
```

**주요 특징:**
- **제네릭 타입 `T`**: 페이로드의 타입을 지정
- **불변성 보장**: 생성 후 페이로드와 헤더 모두 변경 불가
- **구현 클래스**: `GenericMessage`, `ErrorMessage`

#### MessageHeaders 클래스 구조

`MessageHeaders`는 **불변(immutable)** 클래스로 `Map<String, Object>`를 구현합니다.

```java
public class MessageHeaders
    implements Map<String, Object>, Serializable
```

**불변성 강제:**
```java
// 다음 메서드들은 모두 UnsupportedOperationException을 던짐
headers.put("key", "value");        // ❌ 불가능
headers.putAll(map);                // ❌ 불가능
headers.remove("key");              // ❌ 불가능
headers.clear();                    // ❌ 불가능

// 읽기 전용 메서드만 사용 가능
Object value = headers.get("key");  // ✅ 가능
boolean exists = headers.containsKey("key"); // ✅ 가능
```

#### MessageHeaders의 특수 키

| 상수 | 설명 | 생성 시점 |
|------|------|----------|
| **ID** | 메시지 고유 ID (UUID) | 자동 생성 |
| **TIMESTAMP** | 메시지 생성 시각 (Long) | 자동 생성 |
| **CONTENT_TYPE** | 콘텐츠 타입 | 사용자 정의 |
| **REPLY_CHANNEL** | 응답 채널 | 사용자 정의 (선택) |
| **ERROR_CHANNEL** | 에러 채널 | 사용자 정의 (선택) |

**중요:** ID와 TIMESTAMP는 생성 시 자동으로 할당되며, 사용자가 제공한 값은 무시됩니다.

```java
Map<String, Object> headerMap = new HashMap<>();
headerMap.put("custom", "value");
headerMap.put(MessageHeaders.ID, "ignored-value"); // 무시됨

MessageHeaders headers = new MessageHeaders(headerMap);
// headers.getId() → 자동 생성된 UUID
// headers.get("custom") → "value"
```

#### MessageHeaders 내부 저장 구조

```
┌──────────────────────────────────────────────────────────┐
│              MessageHeaders                              │
│          implements Map<String, Object>                  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │        내부 Map<String, Object>                    │ │
│  │                                                    │ │
│  │  시스템 헤더:                                      │ │
│  │    "id"          → UUID (자동 생성)               │ │
│  │    "timestamp"   → 1704297600000L (자동 생성)     │ │
│  │                                                    │ │
│  │  표준 헤더:                                        │ │
│  │    "contentType" → MimeType("application/json")   │ │
│  │    "replyChannel" → MessageChannel 객체            │ │
│  │    "errorChannel" → MessageChannel 객체            │ │
│  │                                                    │ │
│  │  SIMP 헤더 (SimpMessageHeaderAccessor):          │ │
│  │    "simpMessageType"    → MESSAGE                 │ │
│  │    "simpDestination"    → "/topic/greeting"       │ │
│  │    "simpSessionId"      → "abc123"                │ │
│  │    "simpSubscriptionId" → "sub-0"                 │ │
│  │    "simpUser"           → Principal 객체           │ │
│  │                                                    │ │
│  │  Native 헤더 (NativeMessageHeaderAccessor):      │ │
│  │    "nativeHeaders"                                │ │
│  │        → Map<String, List<String>> {              │ │
│  │             "destination": ["/topic/greeting"],   │ │
│  │             "content-type": ["application/json"], │ │
│  │             "message-id": ["msg-001"],            │ │
│  │             "custom-header": ["v1", "v2"]         │ │
│  │           }                                        │ │
│  │                                                    │ │
│  │  사용자 정의 헤더:                                 │ │
│  │    "userId"       → 12345                         │ │
│  │    "priority"     → "high"                        │ │
│  │    "customHeader" → "customValue"                 │ │
│  └────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

#### Message 생성 방법

##### 방법 1: MessageBuilder (권장)

```java
// 기본 사용법
Message<String> message = MessageBuilder
    .withPayload("Hello World")
    .setHeader("userId", 123)
    .setHeader("priority", "high")
    .build();

// 기존 메시지에서 복사
Message<String> newMessage = MessageBuilder
    .fromMessage(existingMessage)
    .setHeader("extraHeader", "value")
    .build();

// MessageHeaderAccessor와 함께 사용
SimpMessageHeaderAccessor accessor =
    SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
accessor.setDestination("/topic/messages");
accessor.setSessionId("session-123");

Message<String> message = MessageBuilder
    .withPayload(payload)
    .setHeaders(accessor)
    .build();
```

##### 방법 2: GenericMessage 직접 생성

```java
Map<String, Object> headers = new HashMap<>();
headers.put("customHeader", "value");

Message<String> message = new GenericMessage<>("payload", headers);
```

##### 방법 3: MessageBuilder.createMessage() (단축)

```java
MessageHeaders headers = new MessageHeaders(headerMap);
Message<String> message = MessageBuilder.createMessage("payload", headers);
```

#### MessageHeaders의 타입 안전 접근

```java
MessageHeaders headers = message.getHeaders();

// 일반 접근 (Object 반환)
Object value = headers.get("userId");

// 타입 안전 접근 (제네릭 메서드)
Integer userId = headers.get("userId", Integer.class);
String priority = headers.get("priority", String.class);
MimeType contentType = headers.get(MessageHeaders.CONTENT_TYPE, MimeType.class);

// 특수 헤더 전용 접근자
UUID id = headers.getId();
Long timestamp = headers.getTimestamp();
Object replyChannel = headers.getReplyChannel();
```

#### Message 객체의 전체 메모리 구조

```
┌─────────────────────────────────────────────────────────────┐
│                    Message<String>                          │
│                (GenericMessage 구현)                        │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Payload: String                                     │  │
│  │  "Hello World"                                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Headers: MessageHeaders                             │  │
│  │                                                      │  │
│  │  ┌────────────────────────────────────────────────┐ │  │
│  │  │  Map<String, Object>                           │ │  │
│  │  │                                                │ │  │
│  │  │  "id" → f47ac10b-58cc-4372-a567-0e02b2c3d479  │ │  │
│  │  │  "timestamp" → 1704297600000                  │ │  │
│  │  │  "simpDestination" → "/topic/greeting"        │ │  │
│  │  │  "simpSessionId" → "abc123"                   │ │  │
│  │  │  "nativeHeaders" → Map {                      │ │  │
│  │  │      "destination": ["/topic/greeting"],      │ │  │
│  │  │      "content-type": ["application/json"]     │ │  │
│  │  │  }                                             │ │  │
│  │  │  "userId" → 12345                             │ │  │
│  │  │  "priority" → "high"                          │ │  │
│  │  └────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### MessageHeaders의 불변성이 주는 이점

1. **스레드 안전성**: 동기화 없이 여러 스레드에서 안전하게 접근 가능
2. **메시지 무결성**: 메시지가 처리 파이프라인을 통과하는 동안 헤더 변경 방지
3. **예측 가능성**: 메시지 상태가 생성 시점부터 일관되게 유지됨
4. **디버깅 용이성**: 메시지 추적 시 헤더 변경 걱정 없음

#### 헤더 수정이 필요한 경우

MessageHeaders는 불변이므로, 헤더를 변경하려면 **새로운 Message를 생성**해야 합니다:

```java
// 기존 메시지
Message<String> original = ...;

// 헤더를 추가/수정한 새 메시지 생성
Message<String> modified = MessageBuilder
    .fromMessage(original)  // 기존 헤더 복사
    .setHeader("newHeader", "newValue")  // 새 헤더 추가
    .build();  // 새 Message 인스턴스 반환
```

## 4. STOMP 프레임 변환 과정

### 4.1 수신 메시지 흐름

```
┌──────────────┐
│ STOMP Client │
└──────┬───────┘
       │ SEND /app/greeting
       │ destination:/app/greeting
       │ content-type:application/json
       │
       ▼
┌─────────────────────────────────────────┐
│ WebSocket Connection                    │
│ (STOMP frame 수신)                      │
└──────┬──────────────────────────────────┘
       │
       │ Decode STOMP frame
       │
       ▼
┌─────────────────────────────────────────┐
│ StompHeaderAccessor                     │
│                                         │
│ 1. Native headers 저장:                │
│    Map<String, List<String>> {          │
│      "destination": ["/app/greeting"]   │
│      "content-type": ["application/json"]│
│    }                                    │
│                                         │
│ 2. SIMP headers 생성:                  │
│    DESTINATION_HEADER = "/app/greeting" │
│    CONTENT_TYPE_HEADER = "application/json"│
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Spring Message                          │
│ - Headers: (SIMP + Native)              │
│ - Payload: JSON body                    │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ clientInboundChannel                    │
│ → @MessageMapping 처리                  │
└─────────────────────────────────────────┘
```

### 4.2 송신 메시지 흐름

```
┌─────────────────────────────────────────┐
│ @MessageMapping 메서드                  │
│ return값: Greeting 객체                 │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Spring Message 생성                     │
│ - Payload: Greeting 객체                │
│ - Default destination header 설정       │
│   ("/app" → "/topic" 변환)              │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ StompHeaderAccessor                     │
│                                         │
│ SIMP headers → Native headers 동기화   │
│ - DESTINATION: "/topic/greeting"        │
│   → native["destination"]: ["/topic/..."]│
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ brokerChannel                           │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ Message Broker                          │
│ (Native headers를 STOMP frame으로 변환) │
└──────┬──────────────────────────────────┘
       │
       ▼
┌──────────────┐
│ STOMP Client │
│ (구독자)     │
└──────────────┘
```

## 5. Native Headers vs SIMP Headers 비교표

| 측면 | Native Headers | SIMP Headers |
|------|----------------|--------------|
| **저장 형태** | `Map<String, List<String>>` | 직접 속성 (typed properties) |
| **관리 클래스** | `NativeMessageHeaderAccessor` | `SimpMessageHeaderAccessor` |
| **목적** | 원시 STOMP 프로토콜 표현 | 프레임워크 정규화된 추상화 |
| **접근 방법** | `getNativeHeaders()` | Typed getter/setter 메서드 |
| **값 형태** | `List<String>` (다중값 가능) | 단일 타입 값 |
| **사용 시점** | STOMP 프레임 인코딩/디코딩 시 | 애플리케이션 로직 처리 시 |
| **동기화** | Message 생성/래핑 시 자동 동기화 | - |

## 6. 주요 Native Header 상수

```java
// STOMP 프로토콜 표준 헤더
public static final String STOMP_ID_HEADER = "id";
public static final String STOMP_HOST_HEADER = "host";
public static final String STOMP_DESTINATION_HEADER = "destination";
public static final String STOMP_CONTENT_TYPE_HEADER = "content-type";
public static final String STOMP_CONTENT_LENGTH_HEADER = "content-length";
public static final String STOMP_LOGIN_HEADER = "login";
public static final String STOMP_PASSCODE_HEADER = "passcode";
public static final String STOMP_ACK_HEADER = "ack";
public static final String STOMP_NACK_HEADER = "nack";
public static final String STOMP_MESSAGE_ID_HEADER = "message-id";
public static final String STOMP_HEARTBEAT_HEADER = "heart-beat";
public static final String STOMP_SUBSCRIPTION_HEADER = "subscription";
```

## 7. 코드 사용 예제

### 7.1 STOMP 프레임에서 Message 생성

```java
// STOMP SEND 프레임 수신 시
StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);

// Native headers에 STOMP 헤더 저장
accessor.setNativeHeader("destination", "/app/greeting");
accessor.setNativeHeader("content-type", "application/json");
accessor.setNativeHeader("custom-header", "custom-value");

// SIMP headers는 자동으로 동기화됨
// accessor.getDestination() == "/app/greeting"
// accessor.getContentType() == MimeType "application/json"

Message<byte[]> message = MessageBuilder.createMessage(
    payload,
    accessor.getMessageHeaders()
);
```

### 7.2 기존 Message 래핑

```java
// SimpMessagingTemplate으로 생성된 메시지를 래핑
Message<?> message = ...;
StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

// 클라이언트로 전송 시 STOMP 명령 업데이트
accessor.updateStompCommandAsClientMessage();

// 브로커로 전송 시
accessor.updateStompCommandAsServerMessage();
```

### 7.3 SimpMessagingTemplate의 기본 동작

공식 문서에 따르면:

> "By default headers are interpreted as native headers (e.g. STOMP) and are saved under a special key in the resulting Spring Message. In effect when the message leaves the application, the provided headers are included with it and delivered to the destination (e.g. the STOMP client or broker)."

```java
@Autowired
private SimpMessagingTemplate messagingTemplate;

public void sendMessage() {
    // 헤더는 기본적으로 Native headers로 해석됨
    Map<String, Object> headers = new HashMap<>();
    headers.put("custom-header", "value");

    // 이 헤더들은 Native headers에 저장되어
    // STOMP 클라이언트나 브로커에 전달됨
    messagingTemplate.convertAndSend("/topic/messages", payload, headers);
}
```

## 8. 핵심 설계 원칙

### 8.1 계층 분리

```
┌─────────────────────────────────────────────┐
│         Application Layer                   │
│    (SIMP Headers로 작업)                    │
│    - 타입 안전                              │
│    - 비즈니스 로직 집중                     │
└──────────────┬──────────────────────────────┘
               │
               │ 자동 동기화
               │
┌──────────────▼──────────────────────────────┐
│         Protocol Layer                      │
│    (Native Headers로 작업)                  │
│    - STOMP 프로토콜 준수                    │
│    - 프레임 인코딩/디코딩                   │
└─────────────────────────────────────────────┘
```

### 8.2 양방향 동기화

- **STOMP → Spring**: Native headers가 먼저 채워지고, SIMP headers가 이를 기반으로 생성됨
- **Spring → STOMP**: SIMP headers 변경 시 Native headers가 자동으로 동기화됨

### 8.3 다중값 지원

Native headers는 `List<String>` 형태로 동일한 헤더의 다중값을 지원:

```java
// Native headers: 다중값 가능
accessor.setNativeHeader("custom-header", "value1");
accessor.addNativeHeader("custom-header", "value2");
// → {"custom-header": ["value1", "value2"]}

// SIMP headers: 단일값
accessor.setDestination("/topic/greeting");
// → DESTINATION_HEADER = "/topic/greeting"
```

## 9. 실무 활용 시나리오

### 9.1 커스텀 헤더 전달

클라이언트에서 서버로 커스텀 헤더 전송:

```javascript
// JavaScript STOMP 클라이언트
stompClient.send("/app/message", {
    "custom-token": "abc123",
    "priority": "high"
}, JSON.stringify(messageBody));
```

```java
// 서버에서 Native headers 접근
@MessageMapping("/message")
public void handleMessage(
    @Header(name = "custom-token", required = false) String token,
    @Header(name = "priority", required = false) String priority,
    Message<?> message
) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

    // Native headers에서 직접 접근
    List<String> tokens = accessor.getNativeHeader("custom-token");
    List<String> priorities = accessor.getNativeHeader("priority");

    // 또는 @Header 애노테이션으로 자동 바인딩
    log.info("Token: {}, Priority: {}", token, priority);
}
```

### 9.2 메시지 전송 시 헤더 설정

```java
@Autowired
private SimpMessagingTemplate messagingTemplate;

public void sendWithHeaders() {
    SimpMessageHeaderAccessor headerAccessor =
        SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

    // SIMP headers 설정
    headerAccessor.setDestination("/topic/notifications");
    headerAccessor.setLeaveMutable(true);

    // Native headers 설정 (클라이언트에 전달됨)
    headerAccessor.setNativeHeader("notification-type", "alert");
    headerAccessor.setNativeHeader("priority", "high");

    messagingTemplate.convertAndSendToUser(
        username,
        "/queue/notifications",
        payload,
        headerAccessor.getMessageHeaders()
    );
}
```

## 10. 참고 자료

### 공식 Spring Framework 문서

1. [Flow of Messages - Spring Framework Reference](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/message-flow.html)
2. [StompHeaderAccessor JavaDoc - Spring Framework 6.2.11 API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/simp/stomp/StompHeaderAccessor.html)
3. [STOMP - Spring Framework Reference](https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html)
4. [SimpMessagingTemplate JavaDoc - Spring Framework 6.0.24 API](https://docs.enterprise.spring.io/spring-framework/docs/6.0.24/javadoc-api/org/springframework/messaging/simp/SimpMessagingTemplate.html)

### 핵심 API 클래스

**메시징 기본 API:**
- [Message](https://docs.spring.io/spring-framework/docs/7.0.2/javadoc-api/org/springframework/messaging/Message.html) - 메시지 인터페이스
- [MessageHeaders](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/MessageHeaders.html) - 불변 메시지 헤더 클래스
- [MessageBuilder](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/support/MessageBuilder.html) - 메시지 빌더
- [MessageHandler](https://docs.spring.io/spring-framework/docs/7.0.2/javadoc-api/org/springframework/messaging/MessageHandler.html) - 메시지 핸들러 인터페이스
- [MessageChannel](https://docs.spring.io/spring-framework/docs/7.0.2/javadoc-api/org/springframework/messaging/MessageChannel.html) - 메시지 채널 인터페이스

**STOMP 전용 API:**
- [StompHeaders](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/simp/stomp/StompHeaders.html) - STOMP 헤더 유틸리티

## 11. 결론

Spring Framework의 STOMP 메시징 아키텍처는 여러 계층으로 헤더를 관리하여 유연성과 안전성을 동시에 제공합니다:

### 핵심 구조

1. **Message 컨테이너**
   - 모든 메시징의 기본 단위
   - 제네릭 타입을 통한 페이로드 타입 안전성
   - 불변 MessageHeaders를 통한 헤더 무결성 보장

2. **MessageHeaders 불변 맵**
   - `Map<String, Object>` 구현으로 다양한 헤더 타입 지원
   - ID와 TIMESTAMP 자동 생성
   - 불변성을 통한 스레드 안전성 및 메시지 무결성 보장

3. **Native Headers**
   - STOMP 프로토콜과의 직접적인 상호작용을 위한 원시 헤더 저장소
   - `Map<String, List<String>>` 형태로 다중값 지원
   - MessageHeaders 내부의 "nativeHeaders" 키에 저장

4. **SIMP Headers**
   - 애플리케이션 로직에서 타입 안전하게 사용할 수 있는 정규화된 헤더
   - MessageHeaders의 개별 키-값으로 직접 저장
   - destination, sessionId, subscriptionId 등 프레임워크 레벨 속성

### 설계 이점

- **계층 분리**: 프로토콜 계층(Native)과 애플리케이션 계층(SIMP) 명확히 구분
- **자동 동기화**: `StompHeaderAccessor`를 통한 양방향 헤더 동기화
- **타입 안전성**: 제네릭과 MessageHeaderAccessor를 통한 컴파일 타임 안전성
- **불변성**: MessageHeaders의 불변 특성으로 예측 가능한 메시지 흐름
- **확장성**: 사용자 정의 헤더와 표준 헤더의 공존 지원

개발자는 이러한 계층 구조를 이해하고 상황에 따라 적절한 계층(Message, MessageHeaders, Native/SIMP headers)을 선택하여 작업할 수 있습니다.
