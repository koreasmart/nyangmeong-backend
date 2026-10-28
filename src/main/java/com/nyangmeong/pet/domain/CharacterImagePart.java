package com.nyangmeong.pet.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [캐릭터 이미지 범위]
 * - full : 전신, face : 얼굴만 (마이페이지 원형)
 */
public enum CharacterImagePart {
    FULL("full"),
    FACE("face");

    private final String value;

    CharacterImagePart(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 쿼리 값으로 조회
     */
    public static Optional<CharacterImagePart> of(String value) {
        return Arrays.stream(values())
                .filter(part -> part.value.equals(value))
                .findFirst();
    }
}
