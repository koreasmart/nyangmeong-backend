package com.nyangmeong.expense.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [영수증 상태 이력 VO]
 * - RECPT_STTS_HSTRY 1:1, 일시 오류 사유는 CHG_RSN (ReceiptFailReason)
 */
@Data
public class ReceiptStatusHistory {
    private Long recptSttsHstrySn;
    private Long recptSn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
