package com.nyangmeong.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - JWT 발급/검증]
 */
class JwtTokenProviderTest {

    private static final byte[] SECRET = "jwt-secret-for-tests-32-bytes-long!!".getBytes();

    private static JwtTokenProvider create(byte[] secret, long accessExpiryMs) {
        JwtProperties properties = new JwtProperties();
        properties.setSecretBase64(Base64.getEncoder().encodeToString(secret));
        properties.setAccessTokenExpiryMs(accessExpiryMs);
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        provider.initializeKey();
        return provider;
    }

    /** 같은 키로 직접 서명한 토큰 */
    private static String sign(String subject, String sid, String typ) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject(subject)
                .expiresAt(Instant.now().plusSeconds(60));
        if (sid != null) {
            claims.claim("sid", sid);
        }
        if (typ != null) {
            claims.claim("typ", typ);
        }
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(SECRET, MacAlgorithm.HS256.getName())));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

    @Test
    void 발급한_토큰에서_회원_번호와_세션_번호를_꺼낸다() {
        JwtTokenProvider provider = create(SECRET, 60_000);

        assertThat(provider.parseAccess(provider.createAccessToken(42L, 9L))).isEqualTo(new TokenClaims(42L, 9L));
        assertThat(provider.parseRefresh(provider.createRefreshToken(42L, 9L))).isEqualTo(new TokenClaims(42L, 9L));
    }

    @Test
    void 헤더는_HS256이고_kid가_없다() {
        String token = create(SECRET, 60_000).createAccessToken(1L, 1L);
        String header = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));

        assertThat(header).contains("\"alg\":\"HS256\"").doesNotContain("kid");
    }

    @Test
    void 만료된_토큰은_TOKEN_EXPIRED다() {
        JwtTokenProvider provider = create(SECRET, -1_000);

        assertErrorCode(() -> provider.parseAccess(provider.createAccessToken(1L, 1L)), ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void 서명이_다르거나_형식이_틀리면_UNAUTHORIZED다() {
        JwtTokenProvider provider = create(SECRET, 60_000);
        JwtTokenProvider other = create("another-secret-for-tests-32-bytes!!!".getBytes(), 60_000);

        assertErrorCode(() -> provider.parseAccess(other.createAccessToken(1L, 1L)), ErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> provider.parseAccess("not.a.jwt"), ErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> provider.parseAccess(""), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 토큰_종류가_다르면_UNAUTHORIZED다() {
        JwtTokenProvider provider = create(SECRET, 60_000);

        assertErrorCode(() -> provider.parseAccess(provider.createRefreshToken(1L, 1L)), ErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> provider.parseRefresh(provider.createAccessToken(1L, 1L)), ErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> provider.parseAccess(sign("1", "1", null)), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void subject나_sid가_숫자가_아니거나_없으면_UNAUTHORIZED다() {
        JwtTokenProvider provider = create(SECRET, 60_000);

        assertThat(provider.parseAccess(sign("1", "2", "access"))).isEqualTo(new TokenClaims(1L, 2L));
        assertErrorCode(() -> provider.parseAccess(sign("jinseong@gmail.com", "1", "access")), ErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> provider.parseAccess(sign("1", "abc", "access")), ErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> provider.parseAccess(sign("1", null, "access")), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 시크릿이_없거나_짧으면_기동_때_실패한다() {
        assertThatThrownBy(() -> create(new byte[0], 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> create("short".getBytes(), 1)).isInstanceOf(IllegalStateException.class);
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
