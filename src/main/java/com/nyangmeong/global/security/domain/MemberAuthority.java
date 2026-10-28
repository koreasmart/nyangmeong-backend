package com.nyangmeong.global.security.domain;

/**
 * [회원 권한 코드]
 * - USRG02 (MBR.MBR_AUTHRT_CD) 와 Spring Security 권한 이름
 */
public enum MemberAuthority {
    USER("USRC0004", "ROLE_USER"),
    ADMIN("USRC0005", "ROLE_ADMIN");

    private final String code;
    private final String role;

    MemberAuthority(String code, String role) {
        this.code = code;
        this.role = role;
    }

    public String getCode() {
        return code;
    }

    public String getRole() {
        return role;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드에 대응하는 권한 이름
     */
    public static String roleOf(String code) {
        return ADMIN.is(code) ? ADMIN.role : USER.role;
    }
}
