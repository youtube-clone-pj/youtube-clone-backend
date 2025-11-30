package com.youtube.notification.service.dto;

public record PushSubscribeRequest(String endpoint, Keys keys, String userAgent) {

    public record Keys(String p256dh, String auth) {
    }
}
