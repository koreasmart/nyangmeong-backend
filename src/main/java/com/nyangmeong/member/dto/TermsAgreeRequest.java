package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [약관별 동의 요청]
 * - POST /api/members 의 trmsAgres 항목, 마케팅 미동의도 false 로 보낸다
 */
public record TermsAgreeRequest(
        @NotNull(message = "약관을 선택해 주세요.") Long trmsSn,
        @NotNull(message = "동의 여부를 보내 주세요.") Boolean agreYn) {
}
