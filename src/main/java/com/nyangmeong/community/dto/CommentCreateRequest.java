package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [댓글 등록 요청]
 * - 대댓글(1depth) 은 prntCmntSn, 대댓글에 단 답글은 원 댓글 스레드에 이어짐 (FN-CMU-001-11)
 */
public record CommentCreateRequest(
        @NotBlank @Size(max = 500) String cmntCn,
        Long prntCmntSn) {
}
