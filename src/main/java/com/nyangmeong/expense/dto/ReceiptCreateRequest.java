package com.nyangmeong.expense.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * [영수증 업로드 요청]
 * - multipart/form-data, 이미지 1건 + 대상 반려동물 (FN-AIM-008-03·04)
 */
public record ReceiptCreateRequest(@NotNull MultipartFile file, @NotNull Long petSn) {
}
