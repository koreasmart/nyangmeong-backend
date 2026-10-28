package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [양육관계 VO]
 * - BRUP_REL 1:1, RPRS_MBR_SN은 생성 컬럼이라 두지 않음
 */
@Data
public class BrupRel {
    private Long brupRelSn;
    private Long petSn;
    private Long mbrSn;
    private String brupRoleCd;
    private String rprsPetYn;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String mdfrId;
}
