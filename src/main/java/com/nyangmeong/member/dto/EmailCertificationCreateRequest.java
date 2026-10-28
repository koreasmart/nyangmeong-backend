package com.nyangmeong.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [이메일 인증번호 발송 요청]
 * - POST /api/email-certifications, 지금은 회원가입 목적(USRC0010)만 받는다
 */
public record EmailCertificationCreateRequest(
        @NotBlank(message = "인증 목적을 보내 주세요.") String emlCertPrpsCd,
        @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 주소 형식을 확인해주세요.") @Size(max = 254) String certTrgtEml) {
}
