package com.nyangmeong.pet.domain;

/**
 * [반려동물 공통코드 그룹]
 * - 등록·수정 요청의 코드 값 검사용
 */
public enum PetCodeGroup {
    PET_SE("PETG01"),
    GENDER("PETG02"),
    COLOR("PETG03");

    private final String code;

    PetCodeGroup(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
