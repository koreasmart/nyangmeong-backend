package com.nyangmeong.community.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [펫이야기 게시글 수정 요청]
 * - multipart/form-data, 보낸 필드만 수정 (FN-CMU-001-15)
 * - keepAtchFileSns 를 보내면 그 목록에 없는 기존 이미지는 삭제
 */
public record PostUpdateRequest(
        String bbsCd,
        @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String pstTtl,
        @Size(max = 2000) @Pattern(regexp = "(?s).*\\S.*") String pstCn,
        List<Long> keepAtchFileSns,
        List<MultipartFile> images) {
}
