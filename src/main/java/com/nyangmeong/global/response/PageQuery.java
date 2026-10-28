package com.nyangmeong.global.response;

/**
 * [목록 요청 조건]
 * - page 는 1부터, size 는 기본 20·최대 50
 * - 사용 예: PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(10))
 */
public record PageQuery(Integer page, Integer size, String sort) {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    public int pageOrDefault() {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    public int sizeOrDefault() {
        return sizeOr(DEFAULT_SIZE);
    }

    /**
     * 화면별 기본 건수를 적용한 size
     */
    public int sizeOr(int defaultSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        return Math.min(size, MAX_SIZE);
    }
}
