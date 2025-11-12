package com.youtube.live.interaction.websocket.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * WebSocket 메시지 핸들러에서 인증된 사용자 정보를 주입받기 위한 어노테이션
 *
 * required = true: 인증 필수
 * required = false: 인증 선택 (비회원 접근 시 null 반환)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
    /**
     * 인증 필수 여부
     * @return true면 인증 필수, false면 선택적 인증
     */
    boolean required() default true;
}
