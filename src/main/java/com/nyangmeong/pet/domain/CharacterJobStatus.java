package com.nyangmeong.pet.domain;

/**
 * [캐릭터 생성 작업 상태 코드]
 * - CHRG01 (CHAR_CRT_JOB.CRT_JOB_STTS_CD)
 */
public enum CharacterJobStatus {
    REQUESTED("CHRC0001"),
    CREATING("CHRC0002"),
    COMPLETED("CHRC0003"),
    FAILED("CHRC0004");

    private final String code;

    CharacterJobStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 진행 중 상태 여부
     */
    public static boolean isInProgress(String code) {
        return REQUESTED.is(code) || CREATING.is(code);
    }
}
