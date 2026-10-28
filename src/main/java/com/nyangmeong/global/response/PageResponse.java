package com.nyangmeong.global.response;

import java.util.List;

import com.github.pagehelper.PageInfo;

/**
 * [목록 응답]
 * - PageHelper 의 PageInfo 에서 필요한 값만 옮긴 구조
 *   : { content, page, size, totalElements, totalPages, hasNext }
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <T> PageResponse<T> of(PageInfo<T> pageInfo) {
        return new PageResponse<>(pageInfo.getList(), pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), pageInfo.isHasNextPage());
    }
}
