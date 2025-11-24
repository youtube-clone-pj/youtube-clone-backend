package com.youtube.notification.domain;


import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class NotificationPolicy {
    private static final int RETENTION_WEEKS = 5;

    public static Instant getVisibleSince() {
        return Instant.now()
                .minus(RETENTION_WEEKS * 7, ChronoUnit.DAYS);
    }
}
