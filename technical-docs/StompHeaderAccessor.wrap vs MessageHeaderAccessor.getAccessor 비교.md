# StompHeaderAccessor.wrap() vs MessageHeaderAccessor.getAccessor() 비교

## 개요

Spring Framework의 WebSocket/STOMP 메시징에서 헤더 정보에 접근하는 두 가지 주요 방법인 `StompHeaderAccessor.wrap()`과 `MessageHeaderAccessor.getAccessor()`의 차이점을 분석합니다.

---

## 메서드 정의

### StompHeaderAccessor.wrap()

```java
/**
 * Create an instance from the payload and headers of the given Message.
 */
public static StompHeaderAccessor wrap(Message<?> message) {
    return new StompHeaderAccessor(message);
}
```

**특징**:
- 항상 **새로운 StompHeaderAccessor 인스턴스를 생성**
- Message의 헤더를 복사하여 새로운 MessageHeaders 객체 생성
- **독립적인 복사본**을 만들어 원본과 분리됨
- null을 반환하지 않음 (항상 새 객체 반환)

### MessageHeaderAccessor.getAccessor()

```java
/**
 * Return a mutable MessageHeaderAccessor for the given message.
 * <p>This method returns the MessageHeaderAccessor instance
 * that is embedded in the message headers, or null if none.
 */
@Nullable
public static <T extends MessageHeaderAccessor> T getAccessor(
        Message<?> message,
        Class<T> requiredType) {

    MessageHeaderAccessor headerAccessor = (MessageHeaderAccessor)
            message.getHeaders().get(MessageHeaderAccessor.ACCESSOR);

    if (headerAccessor != null && requiredType.isAssignableFrom(headerAccessor.getClass())) {
        return (T) headerAccessor;
    }
    return null;
}
```

**특징**:
- Message에 이미 **저장되어 있는 accessor 인스턴스를 반환**
- 헤더를 복사하지 않음 (원본 참조)
- 여러 스레드가 **동일한 accessor 인스턴스를 공유**
- accessor가 없거나 타입이 맞지 않으면 **null 반환 가능**

---

## 내부 동작 비교

### wrap() 호출 시 내부 동작

```java
// 1. wrap() 호출
StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

// 2. 생성자 호출
public StompHeaderAccessor(Message<?> message) {
    super(message);  // 부모 클래스 생성자 호출
}

// 3. MessageHeaderAccessor 생성자
protected MessageHeaderAccessor(Message<?> message) {
    // 헤더를 복사하여 새로운 MessageHeaders 생성
    this.headers = new MessageHeaders(message.getHeaders());
}

// 4. MessageHeaders 생성자
protected MessageHeaders(Map<String, Object> headers, UUID id, Long timestamp) {
    // 원본 headers를 Iterator로 순회하며 복사
    this.headers = (headers != null ? new HashMap<>(headers) : new HashMap<>());
}

// 5. HashMap 복사 생성자
public HashMap(Map<? extends K, ? extends V> m) {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    putMapEntries(m, false);  // Iterator 사용!
}

// 6. putMapEntries() 메서드
final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
    // entrySet().iterator()로 순회하며 복사
    for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
        K key = e.getKey();
        V value = e.getValue();
        putVal(hash(key), key, value, false, evict);
    }
}
```

**핵심**: 6단계의 호출 체인을 거쳐 최종적으로 **HashMap Iterator로 헤더를 순회하며 복사**

### getAccessor() 호출 시 내부 동작

```java
// 1. getAccessor() 호출
StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
        message,
        StompHeaderAccessor.class
);

// 2. 메서드 내부 동작
public static <T extends MessageHeaderAccessor> T getAccessor(
        Message<?> message,
        Class<T> requiredType) {

    // 헤더에서 이미 저장된 accessor를 가져옴 (단순 Map.get() 호출)
    MessageHeaderAccessor headerAccessor = (MessageHeaderAccessor)
            message.getHeaders().get(MessageHeaderAccessor.ACCESSOR);

    if (headerAccessor != null && requiredType.isAssignableFrom(headerAccessor.getClass())) {
        return (T) headerAccessor;  // 기존 객체를 그대로 반환
    }
    return null;
}
```

**핵심**: **단순히 Map.get()으로 기존 accessor를 반환** (복사 없음, Iterator 사용 없음)

---

## 핵심 차이점 비교

| 항목 | `StompHeaderAccessor.wrap()` | `MessageHeaderAccessor.getAccessor()` |
|------|------------------------------|--------------------------------------|
| **객체 생성** | 항상 새 객체 생성 | 기존 객체 반환 |
| **헤더 복사** | 복사함 (new HashMap) | 복사 안 함 (원본 참조) |
| **Iterator 사용** | 사용 (복사 중) | 미사용 |
| **독립성** | 독립적 (복사본) | 원본과 연결됨 |
| **반환 타입** | `StompHeaderAccessor` (non-null) | `StompHeaderAccessor` or `null` |
| **null 가능성** | 없음 | 있음 (null 체크 필요) |
| **성능** | 느림 (6단계 호출 체인, 복사 오버헤드) | 빠름 (단순 Map.get()) |
| **메모리** | 추가 할당 (새 HashMap) | 할당 없음 |
| **타입 안전성** | 높음 (항상 StompHeaderAccessor) | 낮음 (타입 체크 필요) |

---

## ConcurrentModificationException 발생 메커니즘

### wrap() 호출 시 CME 발생 시나리오

```
시간 | Thread A (wrap 호출)                     | Thread B (다른 스레드)
-----|------------------------------------------|---------------------------
t1   | wrap(message) 호출                        |
t2   | → new HashMap<>(headers) 시작            |
t3   | → headers.entrySet().iterator() 생성     |
t4   | → Iterator로 순회 시작                   |
t5   | → iterator.nextNode() 실행 중            | nativeHeaders.put() 실행
     | → modCount 체크 중                       | → modCount 변경!
t6   | ❌ ConcurrentModificationException       |
     | (modCount 불일치 감지)                   |
```

**CME 발생 원인**:
1. `wrap()`이 헤더를 복사하기 위해 HashMap Iterator 사용
2. **복사 중**에 다른 스레드가 원본 헤더 수정
3. HashMap의 **fail-fast 메커니즘**이 modCount 불일치 감지
4. **데이터 무결성 보호**를 위해 ConcurrentModificationException 발생

**중요**: CME는 **방어 로직**입니다. 손상된 데이터가 복사되는 것을 방지하기 위한 안전 장치입니다.

---

## 멀티 스레드 환경에서 헤더 수정이 발생하는 경우

**문제 상황:**

```java
// Thread A: ChannelInterceptor에서 헤더 수정
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message,
            StompHeaderAccessor.class
    );

    if (accessor != null) {
        // 원본 헤더 수정 (nativeHeaders.put() 호출)
        accessor.setUser(new AuthenticatedPrincipal(userId, username));
    }

    return message;
}

// Thread B: EventListener에서 동시에 헤더 접근
@EventListener
public void handleSubscribe(SessionSubscribeEvent event) {
    // ❌ wrap() 사용 시 CME 발생 가능!
    // Thread A가 헤더를 수정하는 동안 복사를 시도하면 CME
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
}
```

**해결 방법: 모두 getAccessor() 사용**

```java
// Thread A: getAccessor()로 헤더 수정
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message,
            StompHeaderAccessor.class
    );

    if (accessor != null) {
        accessor.setUser(new AuthenticatedPrincipal(userId, username));
    }

    return message;
}

// Thread B: getAccessor()로 헤더 읽기
@EventListener
public void handleSubscribe(SessionSubscribeEvent event) {
    // ✅ getAccessor() 사용 - CME 발생 안 함
    // Thread A가 헤더를 수정해도 복사 과정이 없으므로 안전
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            event.getMessage(),
            StompHeaderAccessor.class
    );

    if (accessor != null) {
        String destination = accessor.getDestination();
    }
}
```

**핵심:**
- 어느 한 스레드에서라도 **헤더를 수정**하면
- 다른 스레드에서 `wrap()`을 사용할 경우 **복사 중 CME 발생 가능**
- 따라서 **모든 스레드에서 `getAccessor()` 사용 필수**
- 
---

## 레퍼런스

### Spring Framework 공식 문서

- [MessageHeaderAccessor JavaDoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/support/MessageHeaderAccessor.html) - Spring Framework 공식 API 문서
- [StompHeaderAccessor JavaDoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/simp/stomp/StompHeaderAccessor.html) - STOMP 프로토콜용 헤더 accessor 문서
- [MessageHeaders JavaDoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/MessageHeaders.html) - Message 헤더 구조 문서
- [WebSocket Support](https://docs.spring.io/spring-framework/reference/web/websocket.html) - Spring Framework WebSocket 공식 레퍼런스 가이드

### Spring Framework 소스코드

- [MessageHeaderAccessor.java](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/support/MessageHeaderAccessor.java) - 실제 구현 코드
- [StompHeaderAccessor.java](https://github.com/spring-projects/spring-framework/blob/main/spring-websocket/src/main/java/org/springframework/messaging/simp/stomp/StompHeaderAccessor.java) - STOMP 구현 코드

### Java 공식 문서

- [HashMap JavaDoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html) - HashMap의 fail-fast 메커니즘 설명
- [ConcurrentModificationException JavaDoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ConcurrentModificationException.html) - CME 발생 원인과 해결 방법