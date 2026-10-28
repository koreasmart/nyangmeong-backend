package com.nyangmeong.community.dto;

import com.nyangmeong.community.domain.PostImage;

/**
 * [게시글 이미지 항목]
 * - 이미지 조회 경로는 첨부파일 API로 만든다
 */
public record PostImageSummary(Long atchFileSn, Integer sortSeq) {

    public static PostImageSummary from(PostImage image) {
        return new PostImageSummary(image.getAtchFileSn(), image.getSortSeq());
    }
}
