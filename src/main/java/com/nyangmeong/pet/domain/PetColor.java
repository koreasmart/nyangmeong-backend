package com.nyangmeong.pet.domain;

/**
 * [털색 코드]
 * - PETG03 (PET.COLR_CD), 직접 입력이면 COLR_DRCT_INPT_NM 필수
 */
public enum PetColor {
    DIRECT_INPUT("PETC0017");

    private final String code;

    PetColor(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
