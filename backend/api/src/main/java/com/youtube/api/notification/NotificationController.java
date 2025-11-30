package com.youtube.api.notification;

import com.youtube.common.exception.AuthErrorCode;
import com.youtube.api.config.resolver.Cursor;
import com.youtube.common.CursorQuery;
import com.youtube.common.exception.BaseException;
import com.youtube.notification.service.dto.NotificationReadResponse;
import com.youtube.notification.service.NotificationService;
import com.youtube.notification.service.NotificationQueryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationService notificationService;
    private final NotificationSseManager notificationSseManager;
    private static final String SESSION_USER_ID = "userId";

    @GetMapping
    public ResponseEntity<NotificationReadResponse> getNotifications(
            @Cursor final CursorQuery<Long> cursorQuery,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        final NotificationReadResponse response = notificationQueryService
                .getNotifications(userId, cursorQuery);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(final HttpSession session) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        final long unreadCount = notificationQueryService.getUnreadCount(userId);
        return ResponseEntity.ok(unreadCount);
    }

    @PostMapping("/read-all")
    public ResponseEntity<Long> markAllAsRead(final HttpSession session) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        final long updatedCount = notificationService.markAllAsRead(userId);
        notificationSseManager.sendUnreadCount(userId, 0);

        return ResponseEntity.ok(updatedCount);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(final HttpSession session) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        return notificationSseManager.createConnection(userId);
    }
}
