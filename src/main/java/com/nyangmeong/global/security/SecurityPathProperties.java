package com.nyangmeong.global.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * [인증 없이 여는 경로]
 * - app.security.permit-all-paths (쉼표 구분), 목록 밖은 전부 인증 필요
 * - 키가 없는 PC 대비 기본값
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityPathProperties {

    /** 화이트리스트 경로 패턴 */
    private List<String> permitAllPaths = List.of(
            "/api/auth/dev-tokens",
            "/api/auth/login",
            "/api/auth/login-attempt-limits",
            "/api/auth/captchas",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/files/public/**",
            "/api/terms",
            "/api/terms/*",
            "/api/email-certifications",
            "/api/email-certifications/*/verify",
            "/api/members",
            "/api/members/nickname-availability",
            "/api/members/phone-availability",
            "/api/members/me/nickname",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**");
}
