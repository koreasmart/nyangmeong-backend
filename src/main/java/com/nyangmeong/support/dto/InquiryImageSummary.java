package com.nyangmeong.support.dto;

import com.nyangmeong.support.domain.InquiryPhoto;

/**
 * [문의 첨부 이미지 항목]
 * - 이미지 조회 경로는 첨부파일 API로 만든다
 */
public record InquiryImageSummary(Long atchFileSn, Integer sortSeq) {

    public static InquiryImageSummary from(InquiryPhoto photo) {
        return new InquiryImageSummary(photo.getAtchFileSn(), photo.getSortSeq());
    }
}
