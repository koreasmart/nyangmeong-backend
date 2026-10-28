package com.nyangmeong.member.dto;

import lombok.Data;

/**
 * [약관 목록 항목]
 * - GET /api/terms, 사용 중인 약관만 정렬순서대로
 */
@Data
public class TermsSummary {
    private Long trmsSn;
    private String trmsFixIdntfKey;
    private String trmsVerId;
    private String trmsTtl;
    private boolean esntlAgreYn;
    private Integer sortSeq;
}
