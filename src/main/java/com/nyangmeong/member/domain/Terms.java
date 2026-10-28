package com.nyangmeong.member.domain;

import lombok.Data;

/**
 * [약관 VO]
 * - TRMS 1:1 중 전문 조회에 쓰는 컬럼
 */
@Data
public class Terms {
    private Long trmsSn;
    private String trmsFixIdntfKey;
    private String trmsVerId;
    private String trmsTtl;
    private String trmsCn;
    private String esntlAgreYn;
}
