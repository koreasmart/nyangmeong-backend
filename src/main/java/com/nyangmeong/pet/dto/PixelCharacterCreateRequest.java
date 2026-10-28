package com.nyangmeong.pet.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * [캐릭터 생성 요청]
 * - multipart/form-data, 사진 필수 (FN-MYP-010-08)
 */
public record PixelCharacterCreateRequest(@NotNull MultipartFile photo) {
}
