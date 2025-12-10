package com.youtube.notification.service;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.web-push.enabled", havingValue = "false")
public class NoOpWebPushSender implements WebPushSender {

    @Override
    public CompletableFuture<Response> sendAsync(
            final String endpoint,
            final String p256dh,
            final String auth,
            final String payload) {
        log.debug("WebPush 비활성화 상태 - 발송 스킵. endpoint: {}", endpoint);

        // 현실적인 시나리오: 90% 성공, 5% 구독 만료, 5% 실패
        final int random = ThreadLocalRandom.current().nextInt(100);
        final int statusCode;

        if (random < 90) {
            statusCode = 201;  // 성공 (lastUsedDate UPDATE)
        } else if (random < 95) {
            statusCode = 410;  // 구독 만료 (DB DELETE)
        } else {
            statusCode = 400;  // 실패 (로그만)
        }

        return CompletableFuture.completedFuture(new MockResponse(statusCode));
    }

    /**
     * 성능 테스트용 최소 구현 Response
     */
    @RequiredArgsConstructor
    private static class MockResponse implements Response {
        private final int statusCode;

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        // 나머지 메서드는 사용하지 않으므로 UnsupportedOperationException 던짐
        @Override public String getStatusText() { throw new UnsupportedOperationException(); }
        @Override public byte[] getResponseBodyAsBytes() { throw new UnsupportedOperationException(); }
        @Override public ByteBuffer getResponseBodyAsByteBuffer() { throw new UnsupportedOperationException(); }
        @Override public InputStream getResponseBodyAsStream() { throw new UnsupportedOperationException(); }
        @Override public String getResponseBody(Charset charset) { throw new UnsupportedOperationException(); }
        @Override public String getResponseBody() { throw new UnsupportedOperationException(); }
        @Override public Uri getUri() { throw new UnsupportedOperationException(); }
        @Override public String getContentType() { throw new UnsupportedOperationException(); }
        @Override public String getHeader(CharSequence name) { throw new UnsupportedOperationException(); }
        @Override public List<String> getHeaders(CharSequence name) { throw new UnsupportedOperationException(); }
        @Override public HttpHeaders getHeaders() { throw new UnsupportedOperationException(); }
        @Override public boolean isRedirected() { throw new UnsupportedOperationException(); }
        @Override public List<Cookie> getCookies() { throw new UnsupportedOperationException(); }
        @Override public boolean hasResponseStatus() { throw new UnsupportedOperationException(); }
        @Override public boolean hasResponseHeaders() { throw new UnsupportedOperationException(); }
        @Override public boolean hasResponseBody() { throw new UnsupportedOperationException(); }
        @Override public SocketAddress getRemoteAddress() { throw new UnsupportedOperationException(); }
        @Override public SocketAddress getLocalAddress() { throw new UnsupportedOperationException(); }
    }
}
