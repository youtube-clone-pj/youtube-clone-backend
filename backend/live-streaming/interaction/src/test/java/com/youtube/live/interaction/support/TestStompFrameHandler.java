package com.youtube.live.interaction.support;

import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 테스트용 StompFrameHandler 구현체
 * WebSocket 메시지를 수집하고 테스트에서 검증할 수 있도록 도와줍니다.
 *
 * @param <T> 수신할 메시지 타입
 */
public class TestStompFrameHandler<T> implements StompFrameHandler {

    private final Class<T> messageType;
    private final List<T> messages = new ArrayList<>();

    public TestStompFrameHandler(final Class<T> messageType) {
        this.messageType = messageType;
    }

    @Override
    public Type getPayloadType(final StompHeaders headers) {
        return messageType;
    }

    @Override
    public void handleFrame(final StompHeaders headers, final Object payload) {
        @SuppressWarnings("unchecked")
        final T message = (T) payload;
        messages.add(message);
    }

    public List<T> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public int getMessageCount() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
    }
}
