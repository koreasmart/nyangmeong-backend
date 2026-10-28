package com.nyangmeong.global.external;

/**
 * [캐릭터 생성 외부 API]
 * - 결과 = CharacterSpec 규격 PNG 바이트, 실패 시 예외
 * - 외부 API 미정 -> FakeCharacterGenerator
 */
public interface CharacterGenerator {

    /**
     * 픽셀 캐릭터 이미지 생성
     */
    byte[] generate(CharacterGenerationRequest request);
}
