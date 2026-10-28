package com.nyangmeong.global.security.jwt;

/**
 * [토큰 클레임]
 * - mbrSn : subject(MBR_SN) / sid : 로그인 세션(RFRSH_TKN_SN)
 */
public record TokenClaims(Long mbrSn, Long sid) {
}
