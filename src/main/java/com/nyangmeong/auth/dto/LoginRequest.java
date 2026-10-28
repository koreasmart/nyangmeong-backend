package com.nyangmeong.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [이메일 로그인 요청]
 * - POST /api/auth/login
 */
public record LoginRequest(
        @NotBlank(message = "아이디를 입력해 주세요.") @Email(message = "이메일 주소 형식을 확인해주세요.") @Size(max = 254) String acntEml,
        @NotBlank(message = "비밀번호를 입력해 주세요.") @Size(max = 128) String pswd,
        /** 보안코드를 요구하는 상태에서만 보낸다. 소문자는 서비스가 대문자로 바꾼다 */
        @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "영문·숫자 6글자를 입력해 주세요.") String scrtyCd) {
}
