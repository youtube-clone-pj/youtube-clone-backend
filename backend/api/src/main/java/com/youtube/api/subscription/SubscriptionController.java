package com.youtube.api.subscription;

import com.youtube.core.subscription.service.SubscriptionQueryService;
import com.youtube.core.subscription.service.SubscriptionService;
import com.youtube.api.subscription.dto.SubscribeResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/channels")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionQueryService subscriptionQueryService;
    private static final String SESSION_USER_ID = "userId";

    @PostMapping("/{channelId}/subscriptions")
    public ResponseEntity<SubscribeResponse> subscribe(
            @PathVariable final Long channelId,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다");
        }

        subscriptionService.subscribe(userId, channelId);

        return ResponseEntity.ok(new SubscribeResponse(true));
    }

    @DeleteMapping("/{channelId}/subscriptions")
    public ResponseEntity<SubscribeResponse> unsubscribe(
            @PathVariable final Long channelId,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다");
        }

        subscriptionService.unsubscribe(userId, channelId, Instant.now());

        return ResponseEntity.ok(new SubscribeResponse(false));
    }

    @GetMapping("/{channelId}/subscriptions/status")
    public ResponseEntity<Boolean> getSubscriptionStatus(
            @PathVariable final Long channelId,
            final HttpSession session
    ) {
        final Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.ok(false);
        }

        final boolean isSubscribed = subscriptionQueryService.isSubscribed(userId, channelId);

        return ResponseEntity.ok(isSubscribed);
    }
}
