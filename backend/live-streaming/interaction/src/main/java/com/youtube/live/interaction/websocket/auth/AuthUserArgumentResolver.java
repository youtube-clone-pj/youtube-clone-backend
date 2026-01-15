package com.youtube.live.interaction.websocket.auth;

import com.youtube.common.exception.AuthErrorCode;
import com.youtube.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;

/**
 * @AuthUser 어노테이션이 붙은 파라미터에 인증된 사용자 정보를 주입하는 Resolver
 */
@Slf4j
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class)
                && parameter.getParameterType().equals(LoginUser.class);
    }

    @Override
    public Object resolveArgument(
            final MethodParameter parameter,
            final Message<?> message
    ) throws Exception {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        final Principal principal = accessor.getUser();

        if (principal instanceof UnauthenticatedPrincipal) {
            return handleUnAuthUser(parameter);
        }

        if (principal instanceof AuthenticatedPrincipal) {
            final AuthenticatedPrincipal authenticatedPrincipal = (AuthenticatedPrincipal) principal;
            return new LoginUser(
                    authenticatedPrincipal.getUserId(),
                    authenticatedPrincipal.getUsername(),
                    authenticatedPrincipal.getProfileImageUrl()
            );
        }

        log.error("예상하지 못한 Principal 타입 - type: {}", principal != null ? principal.getClass().getName() : "null");
        throw new BaseException(AuthErrorCode.AUTHENTICATION_PROCESSING_ERROR);
    }

    private Object handleUnAuthUser(final MethodParameter parameter) {
        final AuthUser authUser = parameter.getParameterAnnotation(AuthUser.class);
        if (authUser != null && authUser.required()) {
            log.warn("인증이 필요한 기능에 비회원이 접근했습니다");
            throw new BaseException(AuthErrorCode.LOGIN_REQUIRED);
        }

        return null;
    }
}
