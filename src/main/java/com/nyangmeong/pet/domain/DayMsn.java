package com.nyangmeong.pet.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [일별미션 VO]
 * - DAY_MSN 1:1, MBR_SN = 출제 때의 주양육자(포인트 적립 대상), CMPTN_MBR_SN = 완료한 회원
 */
@Data
public class DayMsn {
    private Long dayMsnSn;
    private Long careMsnSn;
    private Long petSn;
    private Long mbrSn;
    private Long cmptnMbrSn;
    private LocalDate msnCrtrYmd;
    private Integer prgrsNmtm;
    private Integer givePnt;
    private Integer scorPnt;
    private LocalDateTime cmptnDt;
    private String cmptnYn;
    private String clclnCfmtnYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
