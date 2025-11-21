package com.youtube.notification.service.dto;

import com.youtube.common.CursorPage;
import com.youtube.notification.domain.Notification;
import com.youtube.notification.domain.NotificationTargetType;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

public record NotificationReadResponse(
        List<NotificationInfo> notifications,
        Long nextCursor,
        boolean hasNext
) {
    public static NotificationReadResponse of(final CursorPage<Notification, Long> cursorPage) {
        final List<NotificationInfo> notifications = cursorPage.content().stream()
                .map(NotificationInfo::from)
                .toList();

        return new NotificationReadResponse(
                notifications,
                cursorPage.nextCursor(),
                cursorPage.hasNext()
        );
    }

    @Builder
    public record NotificationInfo(
            NotificationTargetType targetType,
            String title,
            String thumbnailUrl,
            String deeplinkUrl,
            Instant createdDate
    ) {
        public static NotificationInfo from(final Notification notification) {
            return NotificationInfo.builder()
                    .targetType(notification.getTargetType())
                    .title(notification.getTitle())
                    .thumbnailUrl(notification.getThumbnailUrl())
                    .deeplinkUrl(notification.getDeeplinkUrl())
                    .createdDate(notification.getCreatedDate())
                    .build();
        }
    }
}
