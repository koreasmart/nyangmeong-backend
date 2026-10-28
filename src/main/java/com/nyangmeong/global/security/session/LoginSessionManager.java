package com.nyangmeong.global.security.session;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.domain.RefreshToken;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenClaims;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * [로그인 세션 발급·회전·폐기]
 * - 로그인 성공 issue / 재발급 rotate / 로그아웃 revoke
 * - RFRSH_TKN 행 하나 = 세션 하나, 행 번호가 토큰의 sid
 */
@Component
@RequiredArgsConstructor
public class LoginSessionManager {

    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenCookies tokenCookies;
    private final TokenHashUtil tokenHashUtil;

    /**
     * 새 세션 발급
     */
    @Transactional
    public void issue(Long mbrSn, HttpServletResponse response) {
        RefreshToken row = new RefreshToken();
        row.setMbrSn(mbrSn);
        /** sid 가 행 번호라 토큰보다 행이 먼저 : 임시 해시로 등록 */
        row.setRfrshTknHash(tokenHashUtil.hash(UUID.randomUUID().toString()));
        refreshTokenMapper.insert(row, jwtProperties.getRefreshTokenExpiryMs() / 1000);

        Long sid = row.getRfrshTknSn();
        String refreshToken = jwtTokenProvider.createRefreshToken(mbrSn, sid);
        refreshTokenMapper.updateHash(sid, tokenHashUtil.hash(refreshToken));

        response.addHeader(HttpHeaders.SET_COOKIE,
                tokenCookies.accessToken(jwtTokenProvider.createAccessToken(mbrSn, sid)).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, tokenCookies.refreshToken(refreshToken).toString());
    }

    /**
     * 세션 회전 (재발급)
     */
    @Transactional
    public Long rotate(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = tokenCookies.read(request, TokenCookies.REFRESH_TOKEN);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw fail(response);
        }
        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parseRefresh(refreshToken);
        } catch (BaseException e) {
            /** 만료도 UNAUTHORIZED : TOKEN_EXPIRED 는 재발급 신호라 반복 방지 */
            throw fail(response);
        }
        RefreshToken row = refreshTokenMapper.selectActiveByHash(tokenHashUtil.hash(refreshToken));
        if (row == null || !row.getRfrshTknSn().equals(claims.sid()) || !row.getMbrSn().equals(claims.mbrSn())) {
            throw fail(response);
        }
        if (refreshTokenMapper.updateDiscard(row.getRfrshTknSn()) != 1) {
            /** 동시 회전에서 진 요청 */
            throw fail(response);
        }
        issue(row.getMbrSn(), response);
        return row.getMbrSn();
    }

    /**
     * 세션 폐기 (로그아웃)
     */
    @Transactional
    public void revoke(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = tokenCookies.read(request, TokenCookies.REFRESH_TOKEN);
        if (refreshToken != null && !refreshToken.isBlank()) {
            /** 만료·위조 토큰도 해시 일치 행만 폐기 */
            refreshTokenMapper.updateDiscardByHash(tokenHashUtil.hash(refreshToken));
        }
        clearCookies(response);
    }

    private CustomException fail(HttpServletResponse response) {
        clearCookies(response);
        return new CustomException(ErrorCode.UNAUTHORIZED);
    }

    private void clearCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, tokenCookies.deleteAccessToken().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, tokenCookies.deleteRefreshToken().toString());
    }
}
