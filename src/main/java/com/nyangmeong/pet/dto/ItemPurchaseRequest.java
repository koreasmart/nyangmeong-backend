package com.nyangmeong.pet.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [아이템 구매 요청]
 * - 담은 아이템 일괄 구매, 같은 아이템 중복은 하나로 처리
 */
public record ItemPurchaseRequest(@NotBlank @Size(max = 100) String idmptDmndId,
        @NotEmpty List<@NotNull Long> itemSns) {
}
