package com.nyangmeong.member.domain;

/**
 * [이메일 인증 목적 코드]
 * - USRG04 (EML_CERT.EML_CERT_PRPS_CD)
 */
public enum EmailCertPurpose {
    SIGNUP("USRC0010"),
    PASSWORD_RESET("USRC0011"),
    RECOVERY("USRC0012");

    private final String code;

    EmailCertPurpose(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
