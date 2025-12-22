# WebSocket SessionSubscribeEvent ConcurrentModificationException 해결

## 핵심 요약

- 원인: 동일 MessageHeaders에 대해
   - 한 스레드는 수정
   - 다른 스레드는 StompHeaderAccessor.wrap()으로 복사 시도
- StompHeaderAccessor.wrap() 내부에서 HashMap Iterator 사용 → fail-fast 발생
- 해결: StompHeaderAccessor.wrap() 대신 MessageHeaderAccessor.getAccessor() 사용
- 결과: 동시 1,000명 구독 시 ConcurrentModificationException 0건

---

## 배경

라이브 스트리밍 성능 테스트 중, 다수의 사용자가 동시에 WebSocket 구독을 시도할 때 `ConcurrentModificationException`이 지속적으로 발생했습니다.

**성능 테스트 환경**
- Gatling을 사용한 부하 테스트
- 동시 접속자: 1,000명
- 모두 동일한 라이브 스트리밍(ID: 1)에 접속
- WebSocket 구독 시도 시 **수십 번 반복적으로 오류 발생**

---

## 문제 정의

### 오류 로그

```
2025-12-18T14:12:54.455Z ERROR 1 --- [youtube-clone] [o-8080-exec-237] o.s.w.s.m.StompSubProtocolHandler
: Error publishing SessionSubscribeEvent[GenericMessage [payload=byte[0], headers={simpMessageType=SUBSCRIBE,
stompCommand=SUBSCRIBE, nativeHeaders={id=[sub-2], destination=[/topic/livestreams/1/viewer-count]},
simpSessionAttributes={clientId=29CCEB24423A6C7E158105EEF28A458A, HTTP.SESSION.ID=..., userId=245, username=loadtest245},
simpSubscriptionId=sub-2, simpDestination=/topic/livestreams/1/viewer-count}]]

java.util.ConcurrentModificationException: null
    at java.base/java.util.HashMap$HashIterator.nextNode(Unknown Source)
    at java.base/java.util.HashMap$EntryIterator.next(Unknown Source)
    at java.base/java.util.Collections$UnmodifiableMap$UnmodifiableEntrySet$1.next(Unknown Source)
    at java.base/java.util.HashMap.putMapEntries(Unknown Source)
    at java.base/java.util.HashMap.<init>(Unknown Source)
    at org.springframework.messaging.MessageHeaders.<init>(MessageHeaders.java:138)
    at org.springframework.messaging.simp.stomp.StompHeaderAccessor.wrap(StompHeaderAccessor.java:523)
    at com.youtube.live.interaction.websocket.event.LiveStreamingViewerCountPublisher.handleSubscribe(LiveStreamingViewerCountPublisher.java:46)
```

### 핵심 스택 트레이스 분석

```
StompHeaderAccessor.wrap(event.getMessage())
→ new StompHeaderAccessor(message)
→ new MessageHeaders(originalHeaders)
→ new HashMap<>(headers)           ← Iterator로 헤더를 순회하며 복사
→ HashMap.putMapEntries()
→ Iterator.nextNode()               ← modCount 불일치 감지 → ConcurrentModificationException 발생!
```

**문제 지점**: `StompHeaderAccessor.wrap()`이 내부적으로 헤더 HashMap을 **복사**하는 과정에서 Iterator를 사용합니다.

---

## 근본 원인 분석

### ConcurrentModificationException 발생 메커니즘

`ConcurrentModificationException`은 HashMap의 **fail-fast** 메커니즘에 의해 발생합니다:

1. HashMap은 구조적 변경(put, remove 등) 시 `modCount` 값을 증가
2. Iterator 생성 시 현재 `modCount` 값을 저장
3. Iterator 순회 중 `modCount`가 변경되면 즉시 `ConcurrentModificationException` 발생

**fail-fast는 데이터 무결성 보호 메커니즘**입니다. 손상된 데이터가 복사되는 것을 방지합니다.

### StompHeaderAccessor.wrap()의 내부 동작

**상속 구조**:
```
StompHeaderAccessor
  ↓ extends
SimpMessageHeaderAccessor (단순 super() 호출)
  ↓ extends
NativeMessageHeaderAccessor (단순 super() 호출)
  ↓ extends
MessageHeaderAccessor
```

**Spring Framework 소스코드 분석 - 문제 발생 지점**:

`wrap()`은 상속 구조를 거쳐 최종적으로 **MessageHeaderAccessor 생성자**에 도달하며, 여기서 헤더 복사가 발생합니다:

```java
// [1단계] MessageHeaderAccessor 생성자 (wrap()이 상속 구조를 거쳐 여기 도달)
protected MessageHeaderAccessor(Message<?> message) {
    this(message.getHeaders());  // private 생성자 호출
}

// [2단계] MessageHeaderAccessor private 생성자
private MessageHeaderAccessor(@Nullable MessageHeaders headers) {
    this.headers = new MutableMessageHeaders(headers);
}

// [3단계] MutableMessageHeaders 생성자 (MessageHeaders를 상속)
public MutableMessageHeaders(@Nullable Map<String, Object> headers) {
   super(headers, ...);
}

// [4단계] MessageHeaders 생성자
protected MessageHeaders(@Nullable Map<String, Object> headers, ...) {
    // ⚠️ 원본 headers를 새 HashMap으로 복사
    this.headers = (headers != null ? new HashMap<>(headers) : new HashMap<>());
}

// [5단계] HashMap 복사 생성자
public HashMap(Map<? extends K, ? extends V> m) {
    this.loadFactor = DEFAULT_LOAD_FACTOR;
    putMapEntries(m, false);  // ⚠️ Iterator 사용!
}

// [6단계] putMapEntries() 메서드 - ConcurrentModificationException 발생 지점!
final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
    // ⚠️ entrySet().iterator()로 순회하며 복사
    for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
        K key = e.getKey();      // ← Iterator.nextNode() 호출
        V value = e.getValue();  // ← 다른 스레드가 수정하면 ConcurrentModificationException!
        putVal(hash(key), key, value, false, evict);
    }
}
```

**핵심 문제**:
- **4단계**에서 **new HashMap<>(headers)** 호출 시 **MessageHeaders Map 전체**를 entrySet().iterator()로 순회하며 복사
- 복사 중에 다른 스레드가 **동일한 MessageHeaders Map에 키-값을 추가/삭제**하면 **ConcurrentModificationException 발생**
- Iterator 순회 중 `modCount` 변경 감지 → fail-fast 메커니즘 작동

### 실제 동시성 충돌 시나리오

이 프로젝트에서 ConcurrentModificationException이 발생한 **실제 원인**은 두 컴포넌트가 동시에 같은 Message의 MessageHeaders에 접근했기 때문입니다:

**충돌하는 두 컴포넌트**:

1. **WebSocketAuthInterceptor** (ChannelInterceptor)
   ```java
   @Override
   public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
       final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
               message,
               StompHeaderAccessor.class
       );

       if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
           accessor.setUser(new AuthenticatedPrincipal(userId, username));  // ✅ 헤더 수정
       }
       return message;
   }
   ```

2. **LiveStreamingViewerCountPublisher** (EventListener)
   ```java
   @EventListener
   public void handleSubscribe(final SessionSubscribeEvent event) {
       final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());  // ❌ 헤더 복사
       // ...
   }
   ```

**동시성 충돌 타임라인**:

```
시간 | Thread A (WebSocketAuthInterceptor)          | Thread B (LiveStreamingViewerCountPublisher)
-----|----------------------------------------------|----------------------------------------------
t1   | preSend() 호출                                |
t2   | → getAccessor()로 accessor 조회              |
t3   | → accessor.setUser() 준비                    | handleSubscribe() 호출
t4   | → MessageHeaders에 키-값 추가 시작            | → wrap() 호출
     |                                              | → new HashMap<>(headers) 시작
     |                                              | → Iterator 생성
t5   | → MessageHeaders.put() 호출                  | → Iterator로 순회 중
     | → modCount 증가! ⚠️                          | → modCount 체크
t6   |                                              | ❌ ConcurrentModificationException!
     |                                              | (Iterator의 expectedModCount ≠ 현재 modCount)
```

**핵심**:
- **WebSocketAuthInterceptor**는 이미 `getAccessor()`를 사용
- **LiveStreamingViewerCountPublisher**가 `wrap()`을 사용하여 헤더 복사 시도
- 두 스레드가 **동시에 같은 Message의 MessageHeaders**에 접근하면서 충돌
- WebSocketAuthInterceptor가 헤더를 수정하는 동안, LiveStreamingViewerCountPublisher가 Iterator로 헤더를 복사하려다 ConcurrentModificationException 발생

---

## 해결 방법

### MessageHeaderAccessor.getAccessor() 사용

**문제의 근본 원인은 헤더 복사**였으므로, **헤더를 복사하지 않는 방법**을 사용합니다.

#### MessageHeaderAccessor.getAccessor() 소스코드 분석

```java
// MessageHeaderAccessor.getAccessor() 동작
public static <T extends MessageHeaderAccessor> T getAccessor(
        MessageHeaders messageHeaders, @Nullable Class<T> requiredType) {

    // ✅ MutableMessageHeaders인 경우 내부 accessor를 직접 가져옴
    if (messageHeaders instanceof MutableMessageHeaders mutableHeaders) {
        MessageHeaderAccessor headerAccessor = mutableHeaders.getAccessor();
        if (requiredType == null || requiredType.isInstance(headerAccessor)) {
            return (T) headerAccessor;  // ✅ 기존 객체를 그대로 반환 (복사 없음!)
        }
    }
    return null;
}
```

**핵심 차이점**:

- `wrap()`: 새로운 accessor 생성 → 여러 단계 체인 → 헤더 복사 → Iterator 사용 → **ConcurrentModificationException 발생 가능**
- `getAccessor()`: 기존 accessor 반환 → 저장된 인스턴스 직접 조회 → **ConcurrentModificationException 발생 불가능**

---

## 구현

### 수정 전 코드

`LiveStreamingViewerCountPublisher.java:46`
```java
@EventListener
public void handleSubscribe(final SessionSubscribeEvent event) {
    // ❌ wrap() 사용 - 헤더 복사로 ConcurrentModificationException 발생
    final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
   ...
}
```

### 수정 후 코드

```java
@EventListener
public void handleSubscribe(final SessionSubscribeEvent event) {
    // ✅ getAccessor() 사용 - 기존 accessor 반환, 헤더 복사 없음
    final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            event.getMessage(),
            StompHeaderAccessor.class
    );
   ...
}
```

---

## 적용 및 결과

### 성능 테스트 결과

**테스트 환경**
- 동시 접속자: 1,000명
- 라이브 스트리밍 ID: 1
- 테스트 도구: Gatling

**수정 전**:
```
- ConcurrentModificationException: 수십 번 발생
- 구독 실패율: 높음
```

**수정 후**:
```
✅ ConcurrentModificationException: 0건
✅ 모든 구독 성공
```

---

## 레퍼런스

### Spring Framework 공식 문서

- [MessageHeaderAccessor JavaDoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/support/MessageHeaderAccessor.html) - MessageHeaderAccessor 공식 API 문서
- [StompHeaderAccessor JavaDoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/simp/stomp/StompHeaderAccessor.html) - StompHeaderAccessor 공식 API 문서

### Spring Framework 소스코드

- [MessageHeaderAccessor.java](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/support/MessageHeaderAccessor.java) - MessageHeaderAccessor 구현 코드
- [MessageHeaders.java](https://github.com/spring-projects/spring-framework/blob/main/spring-messaging/src/main/java/org/springframework/messaging/MessageHeaders.java) - MessageHeaders 구현 코드

### Java 공식 문서

- [HashMap JavaDoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html) - HashMap의 fail-fast 메커니즘과 동시성 관련 설명
- [ConcurrentModificationException JavaDoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ConcurrentModificationException.html) - ConcurrentModificationException 발생 원인과 대응 방법

### 관련 프로젝트 문서

- [StompHeaderAccessor.wrap vs MessageHeaderAccessor.getAccessor 비교](../technical-docs/StompHeaderAccessor.wrap%20vs%20MessageHeaderAccessor.getAccessor%20비교.md) - 두 메서드의 내부 동작 상세 비교 (복잡한 호출 체인 vs 저장된 인스턴스 조회)
- [STOMP 메시징 Native headers vs Simp headers 구조](../technical-docs/STOMP%20메시징%20Native%20headers%20vs%20Simp%20headers%20구조.md) - Message, MessageHeaders, Native/SIMP headers의 계층 구조와 저장 방식