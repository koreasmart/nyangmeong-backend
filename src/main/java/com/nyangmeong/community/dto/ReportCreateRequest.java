package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [게시글·댓글 신고 요청]
 * - pstSn·cmntSn 중 하나만, 사유가 기타면 상세 내용 필수 (FN-CMU-001-13)
 */
public record ReportCreateRequest(
        Long pstSn,
        Long cmntSn,
        @NotBlank String dclrRsnCd,
        @Size(max = 500) String dclrDtlCn) {
}
