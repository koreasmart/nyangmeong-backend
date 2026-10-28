package com.nyangmeong.global.auth;

/**
 * [양육 역할 코드]
 * - PETG05 (BRUP_REL.BRUP_ROLE_CD)
 */
public enum BrupRole {
    PRIMARY("PETC0018"),
    CO_PARENT("PETC0019");

    private final String code;

    BrupRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
