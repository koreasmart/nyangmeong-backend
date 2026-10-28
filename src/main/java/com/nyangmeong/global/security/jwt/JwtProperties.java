package com.nyangmeong.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [JWT 설정]
 * - 시크릿은 application.properties(git 제외)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 서명 키 (Base64, 32바이트 이상) */
    private String secretBase64;

    /** Access 토큰 만료(ms), 기본 30분 */
    private long accessTokenExpiryMs = 30L * 60 * 1000;

    /** Refresh 토큰 만료(ms), 기본 1일 */
    private long refreshTokenExpiryMs = 24L * 60 * 60 * 1000;
}
