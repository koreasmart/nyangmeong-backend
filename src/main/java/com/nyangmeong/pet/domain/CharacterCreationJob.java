package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [캐릭터 생성 작업 VO]
 * - CHAR_CRT_JOB 1:1, 최초 생성과 무료 재생성마다 1행
 */
@Data
public class CharacterCreationJob {
    private Long charCrtJobSn;
    private Long petSn;
    private Long mbrSn;
    private Long otoInqrySn;
    private String crtJobSttsCd;
    private LocalDateTime prcsBgngDt;
    private LocalDateTime prcsEndDt;
    private String failRsn;
    private String idmptDmndId;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
