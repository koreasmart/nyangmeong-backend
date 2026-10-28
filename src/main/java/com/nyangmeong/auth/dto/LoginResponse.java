package com.nyangmeong.auth.dto;

/**
 * [이메일 로그인 응답]
 * - 토큰은 쿠키로만 간다
 * - ncnmReguide : 가입 후 첫 로그인인데 닉네임 설정이 끝나지 않았을 때만 true
 */
public record LoginResponse(boolean ncnmReguide, String mbrNcnm) {
}
