package com.youtube.notification.exception;

import com.youtube.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    PUSH_SUBSCRIPTION_NOT_FOUND("NOTI_001", "푸시 구독 정보를 찾을 수 없습니다", 404),
    PUSH_SEND_FAILED("NOTI_002", "푸시 알림 전송에 실패했습니다", 500),
    NOTIFICATION_NOT_FOUND("NOTI_003", "알림을 찾을 수 없습니다", 404),
    SSE_CONNECTION_FAILED("NOTI_004", "SSE 초기 연결 실패", 500),
    WEB_PUSH_PAYLOAD_CREATION_FAILED("NOTI_005", "WebPush 페이로드 생성 실패", 500);

    private final String code;
    private final String message;
    private final int statusCode;
}
