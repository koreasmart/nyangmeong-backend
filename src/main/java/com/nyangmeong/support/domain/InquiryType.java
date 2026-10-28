package com.nyangmeong.support.domain;

/**
 * [문의 유형 코드]
 * - CSCG02 (OTO_INQRY.INQRY_TYPE_CD)
 */
public enum InquiryType {
    HEALTH_RECORD("CSCC0005"),
    PIXEL_CHARACTER("CSCC0006"),
    PLACE("CSCC0007"),
    COMMUNITY("CSCC0008"),
    POINT_PAYMENT_LEDGER("CSCC0009"),
    ACCOUNT_LOGIN("CSCC0010"),
    ETC("CSCC0011");

    private final String code;

    InquiryType(String code) {
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
        for (InquiryType type : values()) {
            if (type.is(code)) {
                return true;
            }
        }
        return false;
    }
}
