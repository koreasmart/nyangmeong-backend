package com.nyangmeong.expense.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [영수증 확인 후 저장 요청]
 * - 병원명 50자·검진일은 모든 탭에 공통 (FN-AIM-009-02·026)
 * - idmptDmndId : 중복 클릭·재전송 방지 (FN-AIM-009-24)
 * - expndPlcusNm : 가계부 사용처. 선택, 비면 사용처 없이 기록 (FN-AIM-009-10)
 */
public record ReceiptSaveRequest(
        @NotBlank @Size(max = 100) String idmptDmndId,
        @NotBlank @Size(max = 50) String hsptlNm,
        @NotNull LocalDate ckupYmd,
        @Size(max = 80) String expndPlcusNm,
        @NotEmpty(message = "저장할 반려동물을 1마리 이상 골라 주세요") @Valid List<ReceiptSavePetRequest> pets) {

    public String expndPlcusNmOrNull() {
        return expndPlcusNm == null || expndPlcusNm.isBlank() ? null : expndPlcusNm;
    }
}
