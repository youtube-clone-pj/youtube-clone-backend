package com.youtube.notification.testfixtures.builder;

import com.youtube.core.user.domain.User;
import com.youtube.notification.domain.PushSubscription;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.Instant;

@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PushSubscriptionBuilder {

    private Long id;
    private User user;
    private String endpoint = "https://fcm.googleapis.com/fcm/send/test-endpoint";
    private String p256dh = "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQtUbVlUls0VJXg7A8u-Ts1XbjhazAkj7I99e8QcYP7DkM=";
    private String auth = "tBHItJI5svbpez7KI4CCXg==";
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0";
    private Instant lastUsedDate;
    private boolean active = true;

    public static PushSubscriptionBuilder PushSubscription() {
        return new PushSubscriptionBuilder();
    }

    public PushSubscription build() {
        return PushSubscription.builder()
                .id(this.id)
                .user(this.user)
                .endpoint(this.endpoint)
                .p256dh(this.p256dh)
                .auth(this.auth)
                .userAgent(this.userAgent)
                .lastUsedDate(this.lastUsedDate)
                .active(this.active)
                .build();
    }
}