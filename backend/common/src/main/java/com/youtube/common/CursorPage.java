package com.youtube.common;

import java.util.List;
import java.util.function.Function;

/**
 * 커서 기반 페이지네이션 결과를 담는 제네릭 클래스
 *
 * @param content    실제 반환할 데이터 리스트
 * @param nextCursor 다음 페이지를 위한 커서 값
 * @param hasNext    다음 페이지 존재 여부
 * @param <T>        컨텐츠 타입 (엔티티, DTO, Record 등)
 * @param <C>        커서 타입 (Long, String, Instant 등)
 */
public record CursorPage<T, C>(
        List<T> content,
        C nextCursor,
        boolean hasNext
) {

    /**
     * 커서 기반 페이지 생성
     *
     * @param items           조회된 전체 아이템 (요청 size + 1개)
     * @param requestedSize   실제 요청한 페이지 크기
     * @param cursorExtractor 아이템에서 커서를 추출하는 함수
     * @return 커서 페이지 결과
     */
    public static <T, C> CursorPage<T, C> of(
            final List<T> items,
            final int requestedSize,
            final Function<T, C> cursorExtractor
    ) {
        final boolean hasNext = items.size() > requestedSize;
        final List<T> content = hasNext
                ? items.subList(0, requestedSize)
                : items;

        final C nextCursor = hasNext
                ? cursorExtractor.apply(content.get(content.size() - 1))
                : null;

        return new CursorPage<>(content, nextCursor, hasNext);
    }

    /**
     * ID 기반 커서를 사용하는 경우의 편의 메서드
     *
     * @param items         조회된 전체 아이템 (요청 size + 1개)
     * @param requestedSize 실제 요청한 페이지 크기
     * @param idExtractor   아이템에서 ID를 추출하는 함수
     * @return 커서 페이지 결과 (Long 커서)
     */
    public static <T> CursorPage<T, Long> ofId(
            final List<T> items,
            final int requestedSize,
            final Function<T, Long> idExtractor
    ) {
        return of(items, requestedSize, idExtractor);
    }
}
