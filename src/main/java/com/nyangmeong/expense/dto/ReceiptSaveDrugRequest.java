package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [영수증 저장 처방 약품 행]
 * - 금액은 받지 않는다 : 금액 대조는 화면에서만 (FN-AIM-009-03)
 * - 약품명 40자 (FN-AIM-009-26), 복용 상태는 HLTG06(복용중·종료)
 */
public record ReceiptSaveDrugRequest(
        @NotBlank @Size(max = 40) String mdcnNm,
        @NotNull LocalDate mdcnBgngYmd,
        @NotNull LocalDate mdcnEndYmd,
        @NotNull @Positive Integer dayMdcnNmtm,
        @NotBlank @Pattern(regexp = "HLTC001[01]") String prscSttsCd) {
}
