package com.youtube.core.user.exception;

import com.youtube.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER_001", "존재하지 않는 사용자입니다", 404);

    private final String code;
    private final String message;
    private final int statusCode;
}
