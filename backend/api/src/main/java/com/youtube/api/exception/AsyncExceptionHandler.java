package com.youtube.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    @Override
    public void handleUncaughtException(final Throwable ex, final Method method, final Object... params) {
        log.error("비동기 작업 예외 발생 - class: {}, method: {}, params: {}, error: {}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                Arrays.toString(params),
                ex.getMessage(),
                ex
        );
    }
}
