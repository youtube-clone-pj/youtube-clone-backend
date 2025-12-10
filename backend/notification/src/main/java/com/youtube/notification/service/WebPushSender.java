package com.youtube.notification.service;

import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;

public interface WebPushSender {

    CompletableFuture<Response> sendAsync(
            String endpoint,
            String p256dh,
            String auth,
            String payload
    );
}
