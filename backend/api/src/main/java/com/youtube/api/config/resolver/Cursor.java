package com.youtube.api.config.resolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 메서드 파라미터에서 커서 기반 페이지네이션 쿼리를 자동으로 바인딩하는 어노테이션
 *
 * <p>사용 예시:
 * <pre>
 * {@code
 * @GetMapping("/notifications")
 * public NotificationListResponse getNotifications(
 *         @AuthUser final Long userId,
 *         @Cursor final CursorQuery<Long> cursorQuery
 * ) {
 *     return notificationService.getNotifications(userId, cursorQuery);
 * }
 * }
 * </pre>
 *
 * <p>HTTP 요청 예시:
 * <ul>
 *   <li>첫 페이지: {@code GET /api/notifications?size=20}</li>
 *   <li>다음 페이지: {@code GET /api/notifications?cursor=100&size=20}</li>
 * </ul>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cursor {

    /**
     * size 파라미터가 없거나 0 이하일 때 사용할 기본 페이지 크기
     *
     * @return 기본 페이지 크기
     */
    int defaultSize() default 20;
}
