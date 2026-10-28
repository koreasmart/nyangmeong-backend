package com.nyangmeong.global.security.domain;

/**
 * [회원 상태 코드]
 * - USRG01 (MBR.MBR_STTS_CD)
 */
public enum MemberStatus {
    NORMAL("USRC0001"),
    SUSPENDED("USRC0002"),
    WITHDRAWN("USRC0003");

    private final String code;

    MemberStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    public static boolean isInactive(String code) {
        return SUSPENDED.is(code) || WITHDRAWN.is(code);
    }
}
