package com.nyangmeong.pet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [반려동물 등록 요청]
 * - 품종은 vrtySn·vrtyDrctInptNm 중 하나, 털색 직접 입력이면 colrDrctInptNm 필수 (서비스에서 검사)
 */
public record PetCreateRequest(@NotBlank @Size(max = 20) String petNm,
        @NotBlank String petSeCd,
        Long vrtySn,
        @Size(max = 40) String vrtyDrctInptNm,
        @NotNull LocalDate petBrdt,
        @Pattern(regexp = "^[0-9]{0,20}$") String chipNo,
        @NotBlank String petGndrCd,
        @NotNull Boolean nutrYn,
        @NotNull @Positive @Digits(integer = 5, fraction = 3) BigDecimal petBdwg,
        @Positive @Digits(integer = 5, fraction = 3) BigDecimal goalBdwg,
        @NotNull @Positive @Digits(integer = 5, fraction = 3) BigDecimal minBdwg,
        @NotNull @Positive @Digits(integer = 5, fraction = 3) BigDecimal maxBdwg,
        @NotBlank String colrCd,
        @Size(max = 20) String colrDrctInptNm,
        @Size(max = 1000) String chrcCn,
        @Size(max = 1000) String algyCn,
        @Size(max = 1000) String dssCn) {
}
