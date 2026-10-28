package com.nyangmeong.community.dto;

import com.nyangmeong.community.domain.Report;

/**
 * [신고 응답]
 */
public record ReportResponse(Long dclrSn, String dclrPrcsSttsCd) {

    public static ReportResponse from(Report report) {
        return new ReportResponse(report.getDclrSn(), report.getDclrPrcsSttsCd());
    }
}
