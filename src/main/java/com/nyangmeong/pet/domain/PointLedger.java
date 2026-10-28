package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [포인트원장 VO]
 * - PNT_GNLGR 1:1, DLNG_PNT는 적립 양수·사용 음수
 */
@Data
public class PointLedger {
    private Long pntGnlgrSn;
    private Long mbrSn;
    private Long dayMsnSn;
    private Long hldItemSn;
    private String pntDlngTypeCd;
    private Integer dlngPnt;
    private Integer dlngAftrBlncPnt;
    private String memoCn;
    private String idmptDmndId;
    private LocalDateTime regDt;
}
