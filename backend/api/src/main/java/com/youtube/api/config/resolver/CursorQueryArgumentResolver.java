package com.youtube.api.config.resolver;

import com.youtube.common.CursorQuery;
import com.youtube.common.exception.BaseException;
import com.youtube.common.exception.CommonErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link Cursor} 어노테이션이 붙은 파라미터를 자동으로 {@link CursorQuery} 객체로 변환하는 ArgumentResolver
 *
 * <p>HTTP 요청 파라미터 {@code cursor}와 {@code size}를 읽어서 CursorQuery 객체를 생성합니다.
 *
 * <p>파라미터 파싱 규칙:
 * <ul>
 *   <li>{@code cursor}: Long 타입으로 파싱 (null 허용)</li>
 *   <li>{@code size}: Integer 타입으로 파싱 (null 허용, 기본값은 어노테이션의 defaultSize)</li>
 * </ul>
 */
public class CursorQueryArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Cursor.class)
                && CursorQuery.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            final MethodParameter parameter,
            final ModelAndViewContainer mavContainer,
            final NativeWebRequest webRequest,
            final WebDataBinderFactory binderFactory
    ) {
        final Cursor annotation = parameter.getParameterAnnotation(Cursor.class);
        final int defaultSize = annotation != null ? annotation.defaultSize() : 20;

        final String cursorParam = webRequest.getParameter("cursor");
        final String sizeParam = webRequest.getParameter("size");

        final Long cursor = parseCursor(cursorParam);
        final Integer size = parseSize(sizeParam);

        return CursorQuery.ofId(cursor, size, defaultSize);
    }

    private Long parseCursor(final String cursorParam) {
        if (cursorParam == null || cursorParam.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursorParam);
        } catch (NumberFormatException e) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }

    private Integer parseSize(final String sizeParam) {
        if (sizeParam == null || sizeParam.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(sizeParam);
        } catch (NumberFormatException e) {
            throw new BaseException(CommonErrorCode.INVALID_INPUT);
        }
    }
}
