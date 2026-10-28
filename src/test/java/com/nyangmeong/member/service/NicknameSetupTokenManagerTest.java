package com.nyangmeong.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.http.Cookie;

/**
 * [검사 - 닉네임 설정 쿠키]
 * - 서명된 값만 회원번호로 인정 / 위조·만료·없음은 null / 쿠키 속성
 */
class NicknameSetupTokenManagerTest {

    private final NicknameSetupTokenManager manager = manager(30);

    private static NicknameSetupTokenManager manager(long validMinutes) {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setHmacKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        FieldCryptoService crypto = new FieldCryptoService(properties);
        crypto.initializeKeys();
        NicknameSetupTokenManager manager = new NicknameSetupTokenManager(crypto, new TokenCookies(new JwtProperties()));
        ReflectionTestUtils.setField(manager, "validMinutes", validMinutes);
        return manager;
    }

    private MockHttpServletRequest requestWith(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(NicknameSetupTokenManager.COOKIE_NAME, value));
        return request;
    }

    @Test
    void 발급한_쿠키는_회원번호로_되돌릴_수_있다() {
        ResponseCookie cookie = manager.issue(11L);

        assertThat(manager.resolve(requestWith(cookie.getValue()))).isEqualTo(11L);
    }

    @Test
    void 쿠키는_httpOnly이고_회원_API_경로에만_실린다() {
        ResponseCookie cookie = manager.issue(11L);

        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/members");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().toMinutes()).isEqualTo(30);
    }

    @Test
    void 회원번호를_바꾸면_서명이_맞지_않아_인정하지_않는다() {
        String[] parts = manager.issue(11L).getValue().split("\\.");

        assertThat(manager.resolve(requestWith("12." + parts[1] + "." + parts[2]))).isNull();
    }

    @Test
    void 만료시각을_늘려도_인정하지_않는다() {
        String[] parts = manager.issue(11L).getValue().split("\\.");

        assertThat(manager.resolve(requestWith(parts[0] + "." + (Long.parseLong(parts[1]) + 3600) + "." + parts[2])))
                .isNull();
    }

    @Test
    void 만료된_쿠키는_인정하지_않는다() {
        String value = manager(-1).issue(11L).getValue();

        assertThat(manager.resolve(requestWith(value))).isNull();
    }

    @Test
    void 다른_키로_서명한_값은_인정하지_않는다() {
        assertThat(manager.resolve(requestWith("11.9999999999." + "0".repeat(64)))).isNull();
    }

    @Test
    void 쿠키가_없거나_형식이_다르면_null이다() {
        assertThat(manager.resolve(new MockHttpServletRequest())).isNull();
        assertThat(manager.resolve(requestWith("abc"))).isNull();
        assertThat(manager.resolve(requestWith("a.b.c"))).isNull();
    }
}
