package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [닉네임 설정 완료 요청]
 * - PATCH /api/members/me/nickname, 기본 닉네임을 그대로 확정할 때도 현재 값을 보낸다
 */
public record MemberNicknameUpdateRequest(
        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(max = 10, message = "닉네임은 최대 10글자까지 입력할 수 있습니다.")
        @Pattern(regexp = "^[가-힣A-Za-z0-9]*$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.") String mbrNcnm) {
}
