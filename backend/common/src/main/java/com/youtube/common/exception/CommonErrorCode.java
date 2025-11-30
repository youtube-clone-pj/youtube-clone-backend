package com.youtube.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT("COMMON_001", "잘못된 입력값입니다", 400),
    UNAUTHORIZED("COMMON_002", "인증이 필요합니다", 401),
    FORBIDDEN("COMMON_003", "권한이 없습니다", 403),
    NOT_FOUND("COMMON_004", "리소스를 찾을 수 없습니다", 404),
    INTERNAL_ERROR("COMMON_500", "서버 오류가 발생했습니다", 500);

    private final String code;
    private final String message;
    private final int statusCode;
}
