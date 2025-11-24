package com.youtube.notification.domain;

import com.youtube.common.CursorPage;
import com.youtube.common.CursorQuery;
import com.youtube.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationReader {
    private final NotificationRepository notificationRepository;

    public CursorPage<Notification, Long> readByUserIdWithCursor(
            final Long userId,
            final CursorQuery<Long> cursorQuery,
            final Instant visibleSince
    ) {
        final List<Notification> notifications = notificationRepository
                .findByReceiverIdWithCursor(
                        userId,
                        cursorQuery.cursor(),
                        visibleSince,
                        PageRequest.of(0, cursorQuery.fetchSize())
                );

        return CursorPage.ofId(
                notifications,
                cursorQuery.size(),
                Notification::getId
        );
    }
}
