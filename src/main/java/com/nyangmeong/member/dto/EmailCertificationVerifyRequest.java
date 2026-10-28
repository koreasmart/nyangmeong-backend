package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * [이메일 인증번호 확인 요청]
 * - POST /api/email-certifications/{emlCertSn}/verify, 로그인 보안코드(영문·숫자)와 구분되는 숫자 6자리
 */
public record EmailCertificationVerifyRequest(
        @NotBlank(message = "인증번호를 입력해 주세요.") @Pattern(regexp = "^\\d{6}$", message = "인증번호는 숫자 6자리로 입력해 주세요.") String certNo) {
}
