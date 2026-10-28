package com.nyangmeong.support.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [1:1 문의 수정 요청]
 * - multipart/form-data, 보낸 필드만 수정 (FN-CSC-003-15)
 * - keepAtchFileSns 를 보내면 그 목록에 없는 기존 첨부는 삭제
 */
public record InquiryUpdateRequest(
        String inqryTypeCd,
        @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String inqryTtl,
        @Size(max = 2000) @Pattern(regexp = "(?s).*\\S.*") String inqryCn,
        List<Long> keepAtchFileSns,
        List<MultipartFile> images) {
}
