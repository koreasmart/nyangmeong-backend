package com.nyangmeong.global.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [암호화 키 설정]
 * - 값은 application.properties(git 제외)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.crypto")
public class CryptoProperties {

    /** AES-256 키 (Base64, 32바이트) */
    private String aesKeyBase64;

    /** HMAC-SHA256 키 (Base64, 32바이트 이상) */
    private String hmacKeyBase64;
}
