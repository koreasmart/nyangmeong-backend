package com.nyangmeong.pet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.nyangmeong.pet.domain.BrupRel;
import com.nyangmeong.pet.domain.Pet;

/**
 * [반려동물 상세 응답]
 * - 등록/수정 모달 채우기 (SCR-MYP-003)
 * - 체중 잠금은 화면이 건강기록 API로 판단 (docs/api/issues.md 마이페이지 2번)
 */
public record PetResponse(Long petSn, String petNm, String petSeCd, Long vrtySn, String vrtyNm,
        String vrtyDrctInptNm, String petGndrCd, LocalDate petBrdt, BigDecimal petBdwg, String colrCd,
        String colrDrctInptNm, String chipNo, String chrcCn, String algyCn, String dssCn, String brupRoleCd,
        boolean rprsPetYn, boolean nutrYn, BigDecimal goalBdwg, BigDecimal minBdwg, BigDecimal maxBdwg,
        List<CaregiverSummary> caregivers) {

    public static PetResponse from(Pet pet, String vrtyNm, BrupRel rel, List<CaregiverSummary> caregivers) {
        return new PetResponse(pet.getPetSn(), pet.getPetNm(), pet.getPetSeCd(), pet.getVrtySn(), vrtyNm,
                pet.getVrtyDrctInptNm(), pet.getPetGndrCd(), pet.getPetBrdt(), pet.getPetBdwg(), pet.getColrCd(),
                pet.getColrDrctInptNm(), pet.getChipNo(), pet.getChrcCn(), pet.getAlgyCn(), pet.getDssCn(),
                rel.getBrupRoleCd(), "Y".equals(rel.getRprsPetYn()), "Y".equals(pet.getNutrYn()),
                pet.getGoalBdwg(), pet.getMinBdwg(), pet.getMaxBdwg(), caregivers);
    }
}
