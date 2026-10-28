package com.nyangmeong.community.domain;

/**
 * [신고 사유 코드]
 * - CMUG04 (DCLR.DCLR_RSN_CD), 기타는 상세 내용 필수
 */
public enum ReportReason {
    SPAM("CMUC0009"),
    ABUSE("CMUC0010"),
    OBSCENE("CMUC0011"),
    PRIVACY("CMUC0012"),
    FRAUD("CMUC0013"),
    ETC("CMUC0014");

    private final String code;

    ReportReason(String code) {
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
        for (ReportReason reason : values()) {
            if (reason.is(code)) {
                return true;
            }
        }
        return false;
    }
}
