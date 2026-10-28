package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [캐릭터 사진 VO]
 * - CHAR_PHOTO 1:1, 원본(첨부 사진)·결과(생성 이미지)
 */
@Data
public class CharacterPhoto {
    private Long charPhotoSn;
    private Long pixelCharSn;
    private Long charCrtJobSn;
    private Long atchFileSn;
    private String photoRoleCd;
    private Integer sortSeq;
    private String nowIndctYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
