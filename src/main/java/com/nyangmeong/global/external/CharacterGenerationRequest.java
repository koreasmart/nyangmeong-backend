package com.nyangmeong.global.external;

/**
 * [캐릭터 생성 입력]
 * - 첨부 사진 + 반려동물 등록 정보 (FN-MYP-010-08)
 */
public record CharacterGenerationRequest(byte[] photo, String petSeCd, String vrtyNm, String colrNm, String chrcCn) {
}
