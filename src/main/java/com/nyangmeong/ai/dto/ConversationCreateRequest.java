package com.nyangmeong.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [대화 생성 요청]
 * - petSn 이 null 이면 「전체」 대화
 */
public record ConversationCreateRequest(
        Long petSn,
        @NotBlank @Size(max = 200) String cnvrsTtl,
        @NotBlank @Size(max = 1000) String cnvrsCn) {
}
