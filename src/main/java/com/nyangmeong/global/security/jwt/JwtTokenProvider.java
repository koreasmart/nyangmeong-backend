package com.nyangmeong.global.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * [JWT 발급/검증]
 * - subject : MBR_SN / sid : 로그인 세션(RFRSH_TKN_SN) / typ : access·refresh
 * - Spring Security 내장 Nimbus 구현, HS256
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;
    private static final String SID_CLAIM = "sid";
    private static final String TYP_CLAIM = "typ";
    private static final String TYP_ACCESS = "access";
    private static final String TYP_REFRESH = "refresh";

    private final JwtProperties properties;

    private JwtEncoder encoder;

    private JwtDecoder decoder;

    @PostConstruct
    public void initializeKey() {
        String secret = properties.getSecretBase64();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret-base64 값이 없습니다. 디스코드의 application.properties를 확인해 주세요.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.jwt.secret-base64 값이 Base64가 아닙니다.", e);
        }
        if (keyBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret-base64는 Base64로 풀었을 때 32바이트 이상이어야 합니다.");
        }
        SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM.getName());
        /** kid 없는 헤더 */
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(ALGORITHM).build();
        /**
         * 만료 여유 0초
         * - 검증기가 이것 하나라 JwtValidationException = 만료
         */
        nimbusDecoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
        decoder = nimbusDecoder;
    }

    public String createAccessToken(Long mbrSn, Long sid) {
        return create(mbrSn, sid, TYP_ACCESS, properties.getAccessTokenExpiryMs());
    }

    public String createRefreshToken(Long mbrSn, Long sid) {
        return create(mbrSn, sid, TYP_REFRESH, properties.getRefreshTokenExpiryMs());
    }

    /**
     * Access 토큰 검증
     */
    public TokenClaims parseAccess(String token) {
        return parse(token, TYP_ACCESS);
    }

    /**
     * Refresh 토큰 검증
     */
    public TokenClaims parseRefresh(String token) {
        return parse(token, TYP_REFRESH);
    }

    private String create(Long mbrSn, Long sid, String typ, long expiryMs) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expiryMs);
        /** 만료 설정 0 이하 대비 : Nimbus 는 iat 가 exp 보다 앞이어야 함 */
        Instant issuedAt = expiresAt.isAfter(now) ? now : expiresAt.minusSeconds(1);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(mbrSn))
                .claim(SID_CLAIM, String.valueOf(sid))
                .claim(TYP_CLAIM, typ)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(ALGORITHM).build(), claims)).getTokenValue();
    }

    private TokenClaims parse(String token, String expectedTyp) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtValidationException e) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (!expectedTyp.equals(jwt.getClaimAsString(TYP_CLAIM))) {
            /** Refresh 를 Access 자리에 넣은 경우 등 */
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return new TokenClaims(Long.valueOf(jwt.getSubject()), Long.valueOf(jwt.getClaimAsString(SID_CLAIM)));
        } catch (NumberFormatException e) {
            /** subject·sid 형식 불일치 */
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
