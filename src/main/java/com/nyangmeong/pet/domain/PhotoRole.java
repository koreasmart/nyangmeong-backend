package com.nyangmeong.pet.domain;

/**
 * [캐릭터 사진 역할 코드]
 * - FILG02 (CHAR_PHOTO.PHOTO_ROLE_CD)
 */
public enum PhotoRole {
    ORIGINAL("FILC0006"),
    RESULT("FILC0007");

    private final String code;

    PhotoRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
