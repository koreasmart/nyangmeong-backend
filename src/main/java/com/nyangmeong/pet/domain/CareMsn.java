package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [케어미션 VO]
 * - CARE_MSN 1:1
 */
@Data
public class CareMsn {
    private Long careMsnSn;
    private String msnPoolCd;
    private String msnNm;
    private Integer cmpnPnt;
    private Integer achvCrtrNmtm;
    private String mvmnPath;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String rgtrId;
    private String mdfrId;
}
