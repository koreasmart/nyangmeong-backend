package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [신고 상태 이력 VO]
 * - DCLR_STTS_HSTRY 1:1
 */
@Data
public class ReportStatusHistory {
    private Long dclrSttsHstrySn;
    private Long dclrSn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
