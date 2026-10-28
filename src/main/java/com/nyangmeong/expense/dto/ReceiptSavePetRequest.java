package com.nyangmeong.expense.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * [영수증 저장 반려동물 탭]
 * - 저장 안 함 탭은 보내지 않는다 (FN-AIM-009-07)
 * - 검진 종류는 HLTG01, 검진 내용 500자·검진 메모 200자 (FN-AIM-009-26)
 * - expenseYn : 가계부에도 기록 (FN-AIM-009-22). 중복 확인에서 [아니오]를 고른 탭은 false 로 보낸다
 * - schdlSn : 가까운 예정 일정에 연결하기로 고른 일정 (FN-HLT-002-46)
 */
public record ReceiptSavePetRequest(
        @NotNull Long petSn,
        @NotBlank @Pattern(regexp = "HLTC000[1-5]") String ckupTypeCd,
        @Size(max = 500) String ckupCn,
        @Size(max = 200) String ckupMemoCn,
        @PositiveOrZero Long amt,
        Boolean expenseYn,
        Long schdlSn,
        @Valid List<ReceiptSaveDrugRequest> drugs) {

    public List<ReceiptSaveDrugRequest> drugsOrEmpty() {
        return drugs == null ? List.of() : drugs;
    }
}
