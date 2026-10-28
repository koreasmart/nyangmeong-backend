package com.nyangmeong.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [보안코드 발급·새로고침 요청]
 * - POST /api/auth/captchas
 */
public record CaptchaCreateRequest(
        @NotBlank(message = "아이디를 입력해 주세요.") @Email(message = "이메일 주소 형식을 확인해주세요.") @Size(max = 254) String acntEml) {
}
