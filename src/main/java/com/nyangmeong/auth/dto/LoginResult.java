package com.nyangmeong.auth.dto;

import com.nyangmeong.global.error.ErrorCode;

/**
 * [로그인 처리 결과]
 * - 실패도 예외가 아니라 결과로 돌려준다 : 실패 기록(횟수 누적)이 롤백되지 않고, 응답 data 에 상태를 실을 수 있다
 * - 성공이면 mbrSn·response, 실패이면 error·limits
 */
public record LoginResult(Long mbrSn, LoginResponse response, ErrorCode error, LoginAttemptLimitResponse limits) {

    public static LoginResult success(Long mbrSn, LoginResponse response) {
        return new LoginResult(mbrSn, response, null, null);
    }

    public static LoginResult fail(ErrorCode error, LoginAttemptLimitResponse limits) {
        return new LoginResult(null, null, error, limits);
    }

    public boolean succeeded() {
        return error == null;
    }
}
