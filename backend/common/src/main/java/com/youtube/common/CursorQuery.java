package com.youtube.common;

/**
 * 커서 기반 페이지네이션 요청 정보
 *
 * @param cursor    커서 값 (첫 페이지는 null)
 * @param size      클라이언트가 요청한 실제 크기
 * @param fetchSize DB 조회 크기 (size + 1, hasNext 판단용)
 * @param <C>       커서 타입 (Long, String, Instant 등)
 */
public record CursorQuery<C>(
        C cursor,
        int size,
        int fetchSize
) {

    public static <C> CursorQuery<C> of(
            final C cursor,
            final Integer size,
            final int defaultSize
    ) {
        final int pageSize = (size != null && size > 0) ? size : defaultSize;
        return new CursorQuery<>(cursor, pageSize, pageSize + 1);
    }

    public static CursorQuery<Long> ofId(
            final Long cursor,
            final Integer size,
            final int defaultSize
    ) {
        return of(cursor, size, defaultSize);
    }
}
