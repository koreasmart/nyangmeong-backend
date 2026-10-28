package com.nyangmeong.expense.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [영수증 VO]
 * - RECPT 1:1
 */
@Data
public class Receipt {
    private Long recptSn;
    private Long mbrSn;
    private Long atchFileSn;
    private String ocrPrcsSttsCd;
    private String ocrDmndSrcCd;
    private String ocrRspnsJson;
    private LocalDateTime strgDt;
    private String idmptDmndId;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
