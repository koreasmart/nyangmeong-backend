package com.nyangmeong.global.security.jwt;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * [JWT httpOnly 쿠키]
 * - httpOnly : XSS 토큰 탈취 방지
 * - Refresh 쿠키는 path=/api/auth 로 제한
 */
@Component
@RequiredArgsConstructor
public class TokenCookies {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String ACCESS_PATH = "/";
    public static final String REFRESH_PATH = "/api/auth";

    private final JwtProperties jwtProperties;

    /** HTTPS 에서만 전송 (운영 true) */
    @Value("${app.cookie.secure:false}")
    private boolean secure;

    public ResponseCookie accessToken(String token) {
        return build(ACCESS_TOKEN, token, ACCESS_PATH, Duration.ofMillis(jwtProperties.getAccessTokenExpiryMs()));
    }

    public ResponseCookie refreshToken(String token) {
        return build(REFRESH_TOKEN, token, REFRESH_PATH, Duration.ofMillis(jwtProperties.getRefreshTokenExpiryMs()));
    }

    public ResponseCookie deleteAccessToken() {
        return build(ACCESS_TOKEN, "", ACCESS_PATH, Duration.ZERO);
    }

    public ResponseCookie deleteRefreshToken() {
        return build(REFRESH_TOKEN, "", REFRESH_PATH, Duration.ZERO);
    }

    /**
     * 요청 쿠키 값 추출
     */
    public String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
