package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [휴대전화번호 중복확인 조건]
 * - GET /api/members/phone-availability?mblTelno=
 */
public record MemberPhoneQuery(
        @NotBlank(message = "휴대전화번호를 입력해 주세요.") @Size(max = 20, message = "휴대전화번호 형식을 확인해 주세요.") String mblTelno) {
}
