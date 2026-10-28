package com.nyangmeong.pet.domain;

/**
 * [반려동물 구분 코드]
 * - PETG01 (PET.PET_SE_CD)
 */
public enum PetSe {
    DOG("PETC0001"),
    CAT("PETC0002");

    private final String code;

    PetSe(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
