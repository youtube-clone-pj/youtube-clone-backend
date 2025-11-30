package com.youtube.common.exception;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    LOGIN_REQUIRED("AUTH_001", "로그인이 필요합니다", 401),
    ALREADY_REGISTERED_EMAIL("AUTH_002", "이미 가입된 이메일입니다", 409),
    PASSWORD_MISMATCH("AUTH_003", "비밀번호가 일치하지 않습니다", 400),
    INVALID_CREDENTIALS("AUTH_004", "인증 정보가 올바르지 않습니다", 401),
    AUTHENTICATION_PROCESSING_ERROR("AUTH_005", "인증 정보 처리 오류", 500);

    private final String code;
    private final String message;
    private final int statusCode;
}
