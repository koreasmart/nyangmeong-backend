package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [캐릭터 생성 작업 상태 이력 VO]
 * - CHAR_CRT_JOB_STTS_HSTRY 1:1, 백그라운드 처리는 MBR_SN 없음
 */
@Data
public class CharacterJobStatusHistory {
    private Long charCrtJobSttsHstrySn;
    private Long charCrtJobSn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
