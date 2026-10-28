package com.nyangmeong.pet.domain;

/**
 * [미션풀 코드]
 * - MYPG04 (CARE_MSN.MSN_POOL_CD), 하루 8개 = 일상케어 5개 + 활동기반 3개 (FN-MYP-020-03)
 */
public enum CareMissionPool {
    DAILY("MYPC0010"),
    ACTIVITY("MYPC0011");

    private final String code;

    CareMissionPool(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
