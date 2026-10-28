package com.nyangmeong.ai.domain;

/**
 * [대화 메시지 역할 코드]
 * - AIMG06 (AI_CNVRS_MSG.ROLE_CD)
 */
public enum AiMessageRole {
    USER("AIMC0024"),
    AI("AIMC0025");

    private final String code;

    AiMessageRole(String code) {
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
        for (AiMessageRole role : values()) {
            if (role.is(code)) {
                return true;
            }
        }
        return false;
    }
}
