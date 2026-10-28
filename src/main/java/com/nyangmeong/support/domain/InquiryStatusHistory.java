package com.nyangmeong.support.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [문의 상태 이력 VO]
 * - OTO_INQRY_STTS_HSTRY 1:1
 */
@Data
public class InquiryStatusHistory {
    private Long otoInqrySttsHstrySn;
    private Long otoInqrySn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
