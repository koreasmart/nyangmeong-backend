package com.nyangmeong.pet.domain;

import java.util.Arrays;

/**
 * [종별 기본 캐릭터 이미지]
 * - PETG01 (PET.PET_SE_CD) -> classpath 이미지, 알 수 없는 종은 고양이
 * - 미생성·생성 실패·오류 확인 중일 때 표시 (FN-MYP-010-03·12·23)
 */
public enum CharacterDefaultImage {
    DOG("PETC0001", "character/default-dog.png"),
    CAT("PETC0002", "character/default-cat.png");

    private final String code;
    private final String path;

    CharacterDefaultImage(String code, String path) {
        this.code = code;
        this.path = path;
    }

    public String getCode() {
        return code;
    }

    public String getPath() {
        return path;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 반려동물 구분 코드로 조회
     */
    public static CharacterDefaultImage from(String petSeCd) {
        return Arrays.stream(values())
                .filter(image -> image.is(petSeCd))
                .findFirst()
                .orElse(CAT);
    }
}
