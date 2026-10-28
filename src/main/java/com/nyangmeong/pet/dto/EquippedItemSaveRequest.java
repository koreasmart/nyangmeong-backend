package com.nyangmeong.pet.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * [착용 상태 저장 요청]
 * - 요청에 없는 보유 아이템은 착용 해제, 빈 목록 = 전부 해제
 */
public record EquippedItemSaveRequest(@NotNull List<@NotNull Long> hldItemSns) {
}
