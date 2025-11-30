package com.youtube.api.notification;

import com.youtube.common.exception.AuthErrorCode;
import com.youtube.common.exception.BaseException;
import com.youtube.notification.service.PushSubscriptionService;
import com.youtube.notification.service.dto.PushSubscribeRequest;
import com.youtube.notification.service.dto.PushUnsubscribeRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/push")
public class PushSubscriptionController {

    private final PushSubscriptionService pushSubscriptionService;
    private static final String SESSION_USER_ID = "userId";

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(
            @RequestBody final PushSubscribeRequest request,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        pushSubscriptionService.subscribe(userId, request);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(
            @RequestBody final PushUnsubscribeRequest request,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        pushSubscriptionService.unsubscribe(request.endpoint());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivateAllSubscriptions(
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        pushSubscriptionService.deactivateAllSubscriptions(userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reactivate")
    public ResponseEntity<Void> reactivateSubscription(
            @RequestBody final PushUnsubscribeRequest request,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        pushSubscriptionService.reactivateSubscription(userId, request.endpoint());

        return ResponseEntity.ok().build();
    }
}
