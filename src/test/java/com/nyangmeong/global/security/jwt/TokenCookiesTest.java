package com.nyangmeong.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;

/**
 * [검사 - JWT 쿠키]
 */
class TokenCookiesTest {

    private final TokenCookies cookies = new TokenCookies(properties());

    private static JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setAccessTokenExpiryMs(1_800_000);
        properties.setRefreshTokenExpiryMs(86_400_000);
        return properties;
    }

    @Test
    void Access_쿠키는_httpOnly_Lax_path_루트이고_만료는_토큰_만료와_같다() {
        ResponseCookie cookie = cookies.accessToken("token-value");

        assertThat(cookie.getName()).isEqualTo("access_token");
        assertThat(cookie.getValue()).isEqualTo("token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(30));
        assertThat(cookie.isSecure()).isFalse();
    }

    @Test
    void Refresh_쿠키는_auth_경로에만_실린다() {
        ResponseCookie cookie = cookies.refreshToken("refresh-value");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(1));
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void 삭제_쿠키는_MaxAge_0이다() {
        assertThat(cookies.deleteAccessToken().getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookies.deleteAccessToken().getValue()).isEmpty();
        assertThat(cookies.deleteRefreshToken().getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookies.deleteRefreshToken().getPath()).isEqualTo("/api/auth");
    }

    @Test
    void 요청_쿠키에서_이름으로_읽고_없으면_null이다() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other", "x"), new Cookie("access_token", "abc"));

        assertThat(cookies.read(request, TokenCookies.ACCESS_TOKEN)).isEqualTo("abc");
        assertThat(cookies.read(request, TokenCookies.REFRESH_TOKEN)).isNull();
        assertThat(cookies.read(new MockHttpServletRequest(), TokenCookies.ACCESS_TOKEN)).isNull();
    }
}
