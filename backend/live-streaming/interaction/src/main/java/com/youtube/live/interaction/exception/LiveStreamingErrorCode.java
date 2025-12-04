package com.youtube.live.interaction.exception;

import com.youtube.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LiveStreamingErrorCode implements ErrorCode {
    CHAT_NOT_ALLOWED_WHEN_OFFLINE("LIVE_001", "채팅은 라이브 방송 중에만 가능합니다", 400),
    LIVE_STREAMING_NOT_FOUND("LIVE_002", "라이브 스트리밍을 찾을 수 없습니다", 404),
    REACTION_ALREADY_EXISTS("LIVE_003", "이미 반응을 남겼습니다", 409),
    INVALID_LAST_CHAT_ID("LIVE_004", "lastChatId는 양수여야 합니다", 400);

    private final String code;
    private final String message;
    private final int statusCode;
}
