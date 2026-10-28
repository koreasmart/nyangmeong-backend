package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [캐릭터 생성용 반려동물 정보]
 * - 품종·털색 = 직접 입력값 우선, 없으면 마스터 이름
 */
@Data
public class PetProfileSummary {
    private Long petSn;
    private String petNm;
    private String petSeCd;
    private String vrtyNm;
    private String colrNm;
    private String chrcCn;
}
