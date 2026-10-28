package com.nyangmeong.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [로그인 실패 상태 조회 조건]
 * - GET /api/auth/login-attempt-limits?email=
 */
public record LoginAttemptLimitQuery(
        @NotBlank(message = "아이디를 입력해 주세요.") @Email(message = "이메일 주소 형식을 확인해주세요.") @Size(max = 254) String email) {
}
