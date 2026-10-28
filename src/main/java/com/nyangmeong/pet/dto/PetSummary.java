package com.nyangmeong.pet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * [내 반려동물 목록 항목]
 * - GET /api/pets, 주양육·공동양육 모두 (SCR-MYP-001 카드, SCR-MYP-002 슬라이드)
 * - bscCharSbstYn : 캐릭터 오류 확인 중 배지, 캐릭터가 없으면 false
 */
@Data
public class PetSummary {
    private Long petSn;
    private String petNm;
    private String petSeCd;
    private Long vrtySn;
    private String vrtyNm;
    private String vrtyDrctInptNm;
    private String petGndrCd;
    private LocalDate petBrdt;
    private BigDecimal petBdwg;
    private String colrCd;
    private String colrDrctInptNm;
    private String chipNo;
    private String chrcCn;
    private String algyCn;
    private String dssCn;
    private String brupRoleCd;
    private boolean rprsPetYn;
    private Integer caregiverCount;
    private boolean bscCharSbstYn;
}
