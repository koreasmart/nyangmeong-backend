package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [픽셀 캐릭터 목록 항목]
 * - GET /api/pixel-characters, 캐릭터 없는 반려동물은 pixelCharSn 없음
 * - 탭 구분 = brupRoleCd (주양육 PCG / 공동양육 CCG)
 */
@Data
public class PixelCharacterSummary {
    private Long petSn;
    private String petNm;
    private String petSeCd;
    private String brupRoleCd;
    private boolean rprsPetYn;
    private Long pixelCharSn;
    private String crtJobSttsCd;
    private Boolean bscCharSbstYn;
}
