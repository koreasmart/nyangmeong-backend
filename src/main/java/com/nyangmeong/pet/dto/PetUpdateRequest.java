package com.nyangmeong.pet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [반려동물 수정 요청]
 * - 보낸 필드만 수정, 문자열 "" -> 비움 (docs/convention/api.md)
 * - goalBdwg를 비우는 방법은 미정 (docs/api/issues.md 마이페이지 4번)
 */
public record PetUpdateRequest(@Size(min = 1, max = 20) String petNm,
        @Size(min = 1) String petSeCd,
        Long vrtySn,
        @Size(max = 40) String vrtyDrctInptNm,
        LocalDate petBrdt,
        @Pattern(regexp = "^[0-9]{0,20}$") String chipNo,
        @Size(min = 1) String petGndrCd,
        Boolean nutrYn,
        @Positive @Digits(integer = 5, fraction = 3) BigDecimal petBdwg,
        @Positive @Digits(integer = 5, fraction = 3) BigDecimal goalBdwg,
        @Positive @Digits(integer = 5, fraction = 3) BigDecimal minBdwg,
        @Positive @Digits(integer = 5, fraction = 3) BigDecimal maxBdwg,
        @Size(min = 1) String colrCd,
        @Size(max = 20) String colrDrctInptNm,
        @Size(max = 1000) String chrcCn,
        @Size(max = 1000) String algyCn,
        @Size(max = 1000) String dssCn) {
}
