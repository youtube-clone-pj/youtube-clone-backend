package com.youtube.notification.service;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushAsyncService;
import org.asynchttpclient.Response;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Security;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushSenderTest {

    private WebPushSender sut;

    @Mock
    private PushAsyncService pushAsyncService;

    @BeforeAll
    static void setUpClass() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeEach
    void setUp() {
        sut = new WebPushSender(pushAsyncService);
    }

    @Test
    @DisplayName("유효한 구독 정보와 페이로드로 푸시를 전송하면 성공한다")
    void sendAsync_ValidSubscription_Success() throws Exception {
        // given
        final String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        final String p256dh = "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQtUbVlUls0VJXg7A8u-Ts1XbjhazAkj7I99e8QcYP7DkM=";
        final String auth = "tBHItJI5svbpez7KI4CCXg==";
        final String payload = "{\"title\":\"Test Notification\"}";

        final Response mockResponse = mock(Response.class);
        when(mockResponse.getStatusCode()).thenReturn(201);
        when(pushAsyncService.send(any(Notification.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        // when
        final CompletableFuture<Response> result = sut.sendAsync(endpoint, p256dh, auth, payload);

        // then
        assertThat(result).isCompleted();
        assertThat(result.get().getStatusCode()).isEqualTo(201);
        verify(pushAsyncService, times(1)).send(any(Notification.class));
    }

    @Test
    @DisplayName("PushAsyncService에서 예외가 발생하면 실패한 CompletableFuture를 반환한다")
    void sendAsync_ServiceThrowsException_ReturnFailedFuture() throws Exception {
        // given
        final String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
        final String p256dh = "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQtUbVlUls0VJXg7A8u-Ts1XbjhazAkj7I99e8QcYP7DkM=";
        final String auth = "tBHItJI5svbpez7KI4CCXg==";
        final String payload = "{\"title\":\"Test Notification\"}";

        when(pushAsyncService.send(any(Notification.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Push service error")));

        // when
        final CompletableFuture<Response> result = sut.sendAsync(endpoint, p256dh, auth, payload);

        // then
        assertThat(result).isCompletedExceptionally();
    }
}
