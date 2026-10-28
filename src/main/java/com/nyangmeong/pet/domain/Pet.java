package com.nyangmeong.pet.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [반려동물 VO]
 * - PET 1:1
 */
@Data
public class Pet {
    private Long petSn;
    private Long vrtySn;
    private String petSeCd;
    private String petGndrCd;
    private String colrCd;
    private String petNm;
    private String vrtyDrctInptNm;
    private String colrDrctInptNm;
    private String chipNo;
    private String chrcCn;
    private String algyCn;
    private String dssCn;
    private LocalDate petBrdt;
    private BigDecimal petBdwg;
    private BigDecimal goalBdwg;
    private BigDecimal minBdwg;
    private BigDecimal maxBdwg;
    private BigDecimal minMealQty;
    private BigDecimal maxMealQty;
    private BigDecimal minWatrQty;
    private BigDecimal maxWatrQty;
    private String nutrYn;
    private String delYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String rgtrId;
    private String mdfrId;
}
