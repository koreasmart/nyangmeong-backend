package com.nyangmeong.expense.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * [영수증 저장 전 확인 요청]
 * - 가계부 중복(FN-AIM-009-23)·가까운 예정 일정(FN-HLT-002-46) 확인용. 저장하지 않는다
 */
public record ReceiptSaveCheckRequest(
        @NotNull LocalDate ckupYmd,
        @NotEmpty @Valid List<Pet> pets) {

    /**
     * 확인할 탭
     */
    public record Pet(@NotNull Long petSn, @PositiveOrZero Long amt, Boolean expenseYn) {
    }
}
