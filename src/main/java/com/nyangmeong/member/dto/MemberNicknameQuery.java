package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [닉네임 중복확인 조건]
 * - GET /api/members/nickname-availability?mbrNcnm=
 */
public record MemberNicknameQuery(
        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(max = 10, message = "닉네임은 최대 10글자까지 입력할 수 있습니다.")
        @Pattern(regexp = "^[가-힣A-Za-z0-9]*$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.") String mbrNcnm) {
}
