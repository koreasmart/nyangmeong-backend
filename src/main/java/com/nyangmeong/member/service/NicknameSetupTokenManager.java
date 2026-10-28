package com.nyangmeong.member.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * [가입 직후 닉네임 설정 쿠키]
 * - 로그인 권한 없이 방금 만든 본인 회원의 닉네임 설정만 허용 : 값은 회원번호.만료시각.서명
 * - 저장소 없이 서명으로 검증, 위조·만료면 null
 */
@Component
@RequiredArgsConstructor
public class NicknameSetupTokenManager {

    public static final String COOKIE_NAME = "ncnm_setup";
    public static final String COOKIE_PATH = "/api/members";

    private final FieldCryptoService fieldCryptoService;
    private final TokenCookies tokenCookies;

    /** HTTPS 에서만 전송 (운영 true) */
    @Value("${app.cookie.secure:false}")
    private boolean secure;

    /** 쿠키 유효시간(분) */
    @Value("${app.signup.nickname-token-valid-minutes:30}")
    private long validMinutes;

    /**
     * 닉네임 설정 쿠키 발급
     */
    public ResponseCookie issue(Long mbrSn) {
        long expiresAt = Instant.now().plus(Duration.ofMinutes(validMinutes)).getEpochSecond();
        String payload = mbrSn + "." + expiresAt;
        return ResponseCookie.from(COOKIE_NAME, payload + "." + sign(payload))
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofMinutes(validMinutes))
                .build();
    }

    /**
     * 요청 쿠키에서 회원번호 확인 : 없거나 위조·만료면 null
     */
    public Long resolve(HttpServletRequest request) {
        String value = tokenCookies.read(request, COOKIE_NAME);
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 3) {
            return null;
        }
        String payload = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        try {
            if (Long.parseLong(parts[1]) < Instant.now().getEpochSecond()) {
                return null;
            }
            return Long.valueOf(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sign(String payload) {
        return fieldCryptoService.hmac("ncnm-setup:" + payload);
    }
}
