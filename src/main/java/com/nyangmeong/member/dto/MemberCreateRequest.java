package com.nyangmeong.member.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [회원가입 요청]
 * - POST /api/members, 지금은 이메일 가입만 (SNS 가입은 SNS 로그인 마일스톤에서 signupTkn 을 더하고 필수 조건을 푼다)
 * - 비밀번호는 BCrypt 한계(72바이트) 때문에 공백 없는 ASCII 로 제한
 */
public record MemberCreateRequest(
        @NotBlank(message = "가입 경로를 보내 주세요.") String joinPathCd,
        @NotNull(message = "이메일 인증을 완료해 주세요.") Long emlCertSn,
        @NotBlank(message = "인증번호를 입력해 주세요.") @Pattern(regexp = "^\\d{6}$", message = "인증번호는 숫자 6자리로 입력해 주세요.") String certNo,
        @NotBlank(message = "휴대전화번호를 입력해 주세요.") @Size(max = 20, message = "휴대전화번호 형식을 확인해 주세요.") String mblTelno,
        @NotBlank(message = "비밀번호를 입력해 주세요.") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,64}$", message = "비밀번호는 8자 이상, 영문과 숫자를 함께 입력해 주세요.") String pswd,
        @NotBlank(message = "비밀번호 확인을 입력해 주세요.") String pswdCnfrm,
        @NotEmpty(message = "약관 동의 내용을 보내 주세요.") @Valid List<TermsAgreeRequest> trmsAgres,
        /** 같은 가입 시도의 재요청 식별값 : 저장 컬럼이 없어 받기만 한다 (재요청은 인증 사용 기록으로 판별) */
        @Size(max = 100) String idmptDmndId) {
}
