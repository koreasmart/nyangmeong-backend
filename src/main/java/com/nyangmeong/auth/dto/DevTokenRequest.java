package com.nyangmeong.auth.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [개발용 토큰 발급 요청]
 */
public record DevTokenRequest(@NotNull Long mbrSn) {
}
