package com.nyangmeong.community.domain;

/**
 * [신고 처리 상태 코드]
 * - CMUG05 (DCLR.DCLR_PRCS_STTS_CD)
 */
public enum ReportStatus {
    RECEIVED("CMUC0015"),
    COMPLETED("CMUC0016"),
    REJECTED("CMUC0017");

    private final String code;

    ReportStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
