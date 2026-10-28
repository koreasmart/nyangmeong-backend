package com.nyangmeong.member.dto;

import com.nyangmeong.member.domain.Terms;

/**
 * [약관 전문 응답]
 * - GET /api/terms/{trmsSn}, 전문은 길이 때문에 자르지 않는다
 */
public record TermsResponse(Long trmsSn, String trmsFixIdntfKey, String trmsVerId, String trmsTtl, String trmsCn,
        boolean esntlAgreYn) {

    public static TermsResponse from(Terms terms) {
        return new TermsResponse(terms.getTrmsSn(), terms.getTrmsFixIdntfKey(), terms.getTrmsVerId(),
                terms.getTrmsTtl(), terms.getTrmsCn(), "Y".equals(terms.getEsntlAgreYn()));
    }
}
