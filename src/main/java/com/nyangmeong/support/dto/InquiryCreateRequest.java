package com.nyangmeong.support.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [1:1 문의 작성 요청]
 * - multipart/form-data, 이미지 최대 10개 (FN-CSC-003-06·08·09)
 * - pixelCharSn·charPhotoSn 이 있으면 캐릭터 오류 문의 (FN-CSC-003-10)
 */
public record InquiryCreateRequest(
        @NotBlank String inqryTypeCd,
        @NotBlank @Size(max = 50) String inqryTtl,
        @NotBlank @Size(max = 2000) String inqryCn,
        List<MultipartFile> images,
        Long pixelCharSn,
        Long charPhotoSn) {
}
