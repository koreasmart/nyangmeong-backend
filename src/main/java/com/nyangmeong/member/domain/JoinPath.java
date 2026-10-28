package com.nyangmeong.member.domain;

/**
 * [가입 경로 코드]
 * - USRG03 (MBR.JOIN_PATH_CD)
 */
public enum JoinPath {
    EMAIL("USRC0006"),
    NAVER("USRC0007"),
    KAKAO("USRC0008"),
    GOOGLE("USRC0009");

    private final String code;

    JoinPath(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
