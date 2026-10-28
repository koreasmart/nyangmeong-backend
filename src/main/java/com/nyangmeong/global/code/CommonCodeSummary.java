package com.nyangmeong.global.code;

import lombok.Data;

/**
 * [공통코드 목록 항목]
 * - GET /api/common-codes
 */
@Data
public class CommonCodeSummary {
    private Long comCdSn;
    private String comCd;
    private String comCdNm;
    private Integer sortSeq;
}
