package com.nyangmeong.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [대화 메시지 추가 요청]
 * - cmptnYn 이 null 이면 true
 */
public record ConversationMessageCreateRequest(
        @NotBlank String roleCd,
        @NotBlank @Size(max = 1000) String cnvrsCn,
        Boolean cmptnYn) {
}
