package com.nyangmeong.support.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [1:1 문의 VO]
 * - OTO_INQRY 1:1
 */
@Data
public class Inquiry {
    private Long otoInqrySn;
    private Long mbrSn;
    private Long prcsMbrSn;
    private Long pixelCharSn;
    private Long charPhotoSn;
    private String inqryTypeCd;
    private String inqryPrcsSttsCd;
    private String charErrPrcsRsltCd;
    private String inqryTtl;
    private String inqryCn;
    private String ansCn;
    private LocalDateTime ansDt;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
