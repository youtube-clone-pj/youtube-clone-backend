package com.youtube.api.exception;

import com.youtube.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(final BaseException e) {
        log.warn("비즈니스 예외 발생 - code: {}, message: {}",
                e.getErrorCode().getCode(), e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getStatusCode())
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e) {
        log.error("예상하지 못한 예외 발생", e);

        return ResponseEntity
                .status(500)
                .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다"));
    }
}
