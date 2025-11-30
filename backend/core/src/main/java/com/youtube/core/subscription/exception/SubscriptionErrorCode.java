package com.youtube.core.subscription.exception;

import com.youtube.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {
    SELF_SUBSCRIPTION_NOT_ALLOWED("SUB_001", "자기 자신의 채널은 구독할 수 없습니다", 400),
    ALREADY_SUBSCRIBED("SUB_002", "이미 구독 중입니다", 409),
    NOT_SUBSCRIBED("SUB_003", "구독하지 않은 채널입니다", 404);

    private final String code;
    private final String message;
    private final int statusCode;
}
