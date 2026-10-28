package com.nyangmeong.community.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [펫이야기 게시글 작성 요청]
 * - multipart/form-data, 이미지 최대 10장 (FN-CMU-001-14)
 */
public record PostCreateRequest(
        @NotBlank String bbsCd,
        @NotBlank @Size(max = 50) String pstTtl,
        @NotBlank @Size(max = 2000) String pstCn,
        List<MultipartFile> images) {
}
