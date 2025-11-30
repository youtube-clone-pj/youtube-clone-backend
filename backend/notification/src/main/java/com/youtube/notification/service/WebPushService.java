package com.youtube.notification.service;

import com.youtube.notification.domain.PushSubscription;
import com.youtube.notification.domain.PushSubscriptionReader;
import com.youtube.notification.domain.PushSubscriptionWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionReader pushSubscriptionReader;
    private final PushSubscriptionWriter pushSubscriptionWriter;
    private final WebPushSender webPushSender;

    public void sendNotification(final Long userId, final String payload) {
        final List<PushSubscription> pushSubscriptions =
                pushSubscriptionReader.readAllByUserIdAndActive(userId, true);

        pushSubscriptions.forEach(pushSubscription -> {
            webPushSender.sendAsync(
                    pushSubscription.getEndpoint(),
                    pushSubscription.getP256dh(),
                    pushSubscription.getAuth(),
                    payload
            )
            .thenAccept(response -> handleResponse(response, pushSubscription, userId))
            .exceptionally(throwable -> {
                handleNetworkFailure(pushSubscription, userId, throwable);
                return null;
            });
        });
    }

    /**
     * WebPush 응답 처리
     *
     * "It checks for status codes 404 and 410, which are the HTTP status codes for 'Not Found' and 'Gone'.
     * If we receive one of these, it means the subscription has expired or is no longer valid.
     * In these scenarios, we need to remove the subscriptions from our database."
     *
     * Reference: https://web.dev/articles/sending-messages-with-web-push-libraries
     */
    private void handleResponse(final org.asynchttpclient.Response response, final PushSubscription pushSubscription, final Long userId) {
        final int statusCode = response.getStatusCode();

        if (shouldDeleteSubscription(statusCode)) {
            pushSubscriptionWriter.hardDelete(pushSubscription.getId());
            log.info("구독 만료로 삭제 - userId: {}, statusCode: {}, endpoint: {}",
                    userId, statusCode, pushSubscription.getEndpoint());
        } else if (statusCode >= 200 && statusCode < 300) {
            handleSuccess(pushSubscription, userId);
        } else {
            log.warn("WebPush 전송 실패 - userId: {}, statusCode: {}, endpoint: {}",
                    userId, statusCode, pushSubscription.getEndpoint());
        }
    }

    private void handleSuccess(final PushSubscription pushSubscription, final Long userId) {
        pushSubscriptionWriter.updateLastUsedDate(pushSubscription.getId());

        log.info("WebPush 전송 성공 - userId: {}, pushSubscriptionId: {}, endpoint: {}",
                userId, pushSubscription.getId(), pushSubscription.getEndpoint());
    }

    /**
     * 410 Gone: 구독이 영구적으로 만료됨
     * 404 Not Found: 구독 엔드포인트가 존재하지 않음
     */
    private boolean shouldDeleteSubscription(final int statusCode) {
        return statusCode == 404 || statusCode == 410;
    }

    private void handleNetworkFailure(final PushSubscription pushSubscription, final Long userId, final Throwable throwable) {
        log.error("WebPush 네트워크 오류 - userId: {}, endpoint: {}, error: {}",
                userId, pushSubscription.getEndpoint(), throwable.getMessage(), throwable);
    }
}
