package com.nyangmeong.expense.dto;

import java.util.List;

/**
 * [영수증 저장 결과]
 * - 프론트가 대표 반려동물의 검진상세로 이동 (FN-AIM-009-12)
 */
public record ReceiptSaveResponse(Long recptSn, List<CheckupRecord> ckupRcds) {

    /**
     * 만들어진 검진기록
     */
    public record CheckupRecord(Long petSn, Long ckupRcdSn) {
    }
}
