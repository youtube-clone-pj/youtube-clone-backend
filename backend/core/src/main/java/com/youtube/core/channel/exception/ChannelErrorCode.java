package com.youtube.core.channel.exception;

import com.youtube.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelErrorCode implements ErrorCode {
    CHANNEL_NOT_FOUND("CHANNEL_001", "존재하지 않는 채널입니다", 404),
    USER_CHANNEL_NOT_FOUND("CHANNEL_002", "해당 유저의 채널이 존재하지 않습니다", 404);

    private final String code;
    private final String message;
    private final int statusCode;
}
