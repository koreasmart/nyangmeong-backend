package com.nyangmeong.auth.dto;

/**
 * [로그인 실패 횟수·보안코드 요구 상태]
 * - GET /api/auth/login-attempt-limits 의 응답이고, 로그인 실패 응답의 data 로도 간다
 * - 기록 없는 아이디는 false·0 : 가입 여부를 드러내지 않는다
 */
public record LoginAttemptLimitResponse(boolean scrtyCdRqtYn, int cntnuCertFailNmtm) {
}
