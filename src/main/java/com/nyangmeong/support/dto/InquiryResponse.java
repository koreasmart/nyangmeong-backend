package com.nyangmeong.support.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.support.domain.Inquiry;

/**
 * [1:1 문의 응답]
 * - 상세·작성·수정 공통, 답변 전에는 ansCn·ansDt 없음
 */
public record InquiryResponse(
        Long otoInqrySn,
        String inqryTypeCd,
        String inqryTypeCdNm,
        String inqryPrcsSttsCd,
        String inqryPrcsSttsCdNm,
        String inqryTtl,
        String inqryCn,
        List<InquiryImageSummary> imgs,
        LocalDateTime regDt,
        String ansCn,
        LocalDateTime ansDt) {

    public static InquiryResponse from(Inquiry inquiry, String inqryTypeCdNm, String inqryPrcsSttsCdNm,
            List<InquiryImageSummary> imgs) {
        return new InquiryResponse(inquiry.getOtoInqrySn(), inquiry.getInqryTypeCd(), inqryTypeCdNm,
                inquiry.getInqryPrcsSttsCd(), inqryPrcsSttsCdNm, inquiry.getInqryTtl(), inquiry.getInqryCn(), imgs,
                inquiry.getRegDt(), inquiry.getAnsCn(), inquiry.getAnsDt());
    }
}
