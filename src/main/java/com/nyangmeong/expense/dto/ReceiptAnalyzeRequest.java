package com.nyangmeong.expense.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [영수증 다시 분석 요청]
 * - 고른 이미지 유지, 대상 반려동물만 다시 받음 (FN-AIM-008-07)
 */
public record ReceiptAnalyzeRequest(@NotNull Long petSn) {
}
