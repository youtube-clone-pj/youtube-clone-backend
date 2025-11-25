package com.youtube.notification.service;

import com.youtube.notification.domain.NotificationWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationWriter notificationWriter;

    public long markAllAsRead(final Long userId) {
        return notificationWriter.markAllAsRead(userId);
    }
}
