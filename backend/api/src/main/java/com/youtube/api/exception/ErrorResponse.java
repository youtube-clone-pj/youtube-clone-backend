package com.youtube.api.exception;

import com.youtube.common.exception.ErrorCode;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(final ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    public static ErrorResponse of(final String code, final String message) {
        return new ErrorResponse(code, message);
    }
}
