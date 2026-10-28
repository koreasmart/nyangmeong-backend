package com.nyangmeong.pet.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [포인트 내역 행]
 * - GET /point-ledgers, 최신순 (FN-MYP-020-02)
 * - dlngPnt 부호로 적립·사용 구분
 */
@Data
public class PointLedgerSummary {
    private Long pntGnlgrSn;
    private String pntDlngTypeCd;
    private String pntDlngTypeCdNm;
    private Integer dlngPnt;
    private String memoCn;
    private LocalDateTime regDt;
}
