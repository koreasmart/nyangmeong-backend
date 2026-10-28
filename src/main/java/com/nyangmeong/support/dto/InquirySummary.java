package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [1:1 문의 목록 항목]
 * - GET /api/inquiries (FN-CSC-003-01)
 */
@Data
public class InquirySummary {
    private Long otoInqrySn;
    private String inqryTypeCd;
    private String inqryTypeCdNm;
    private String inqryTtl;
    private String inqryPrcsSttsCd;
    private String inqryPrcsSttsCdNm;
    private LocalDateTime regDt;
}
