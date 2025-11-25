package com.youtube.notification.service;

import com.youtube.common.CursorPage;
import com.youtube.common.CursorQuery;
import com.youtube.notification.service.dto.NotificationReadResponse;
import com.youtube.notification.domain.Notification;
import com.youtube.notification.domain.NotificationPolicy;
import com.youtube.notification.domain.NotificationReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationReader notificationReader;

    public NotificationReadResponse getNotifications(
            final Long userId,
            final CursorQuery<Long> cursorQuery
    ) {
        final CursorPage<Notification, Long> page = notificationReader
                .readByUserIdWithCursor(userId, cursorQuery, NotificationPolicy.getVisibleSince());

        return NotificationReadResponse.of(page);
    }

    public long getUnreadCount(final Long userId) {
        return notificationReader.countUnreadBy(userId);
    }
}
