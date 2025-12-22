package com.youtube.notification.service;

import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushAsyncService;
import nl.martijndwars.webpush.Subscription;
import org.asynchttpclient.Response;
import org.jose4j.lang.JoseException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "notification.web-push.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class HttpWebPushSender implements WebPushSender {

    private final PushAsyncService pushAsyncService;

    @Override
    public CompletableFuture<Response> sendAsync(
            final String endpoint,
            final String p256dh,
            final String auth,
            final String payload) {
        try {
            final Subscription subscription = new Subscription(
                    endpoint,
                    new Subscription.Keys(p256dh, auth)
            );

            return pushAsyncService.send(new Notification(subscription, payload));
        } catch (GeneralSecurityException | IOException | JoseException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}