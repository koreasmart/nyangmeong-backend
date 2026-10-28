package com.nyangmeong.support.domain;

/**
 * [문의 처리 상태 코드]
 * - CSCG03 (OTO_INQRY.INQRY_PRCS_STTS_CD), 접수는 화면에서 「미답변」
 */
public enum InquiryStatus {
    RECEIVED("CSCC0012"),
    ANSWERED("CSCC0013");

    private final String code;

    InquiryStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 허용 코드 여부
     */
    public static boolean exists(String code) {
        for (InquiryStatus status : values()) {
            if (status.is(code)) {
                return true;
            }
        }
        return false;
    }
}
