package com.nyangmeong.global.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * [개인정보 필드 암복호화]
 * - AES-256-GCM 암호문 형식 : v1.{nonce}.{암호문+태그} (Base64URL)
 * - 중복 확인은 HMAC-SHA256 컬럼
 */
@Component
@RequiredArgsConstructor
public class FieldCryptoService {

    private static final String VERSION = "v1";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_BYTES = 32;
    private static final int GCM_NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int HMAC_MINIMUM_KEY_BYTES = 32;

    private final CryptoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKey aesKey;
    private byte[] hmacKey;

    /**
     * 키 검증 및 초기화
     */
    @PostConstruct
    public void initializeKeys() {
        byte[] aesKeyBytes = decodeRequiredKey(properties.getAesKeyBase64(), "app.crypto.aes-key-base64");
        if (aesKeyBytes.length != AES_KEY_BYTES) {
            throw new IllegalStateException("app.crypto.aes-key-base64는 Base64로 풀었을 때 32바이트여야 합니다.");
        }
        byte[] hmacKeyBytes = decodeRequiredKey(properties.getHmacKeyBase64(), "app.crypto.hmac-key-base64");
        if (hmacKeyBytes.length < HMAC_MINIMUM_KEY_BYTES) {
            throw new IllegalStateException("app.crypto.hmac-key-base64는 Base64로 풀었을 때 32바이트 이상이어야 합니다.");
        }
        aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        hmacKey = hmacKeyBytes;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] nonce = new byte[GCM_NONCE_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + "." + base64Url(nonce) + "." + base64Url(encrypted);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("개인정보를 암호화하지 못했습니다.", e);
        }
    }

    /**
     * 복호화
     */
    public String decrypt(String envelope) {
        if (envelope == null) {
            return null;
        }
        try {
            String[] parts = envelope.split("\\.", -1);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("지원하지 않는 암호문 형식");
            }
            byte[] nonce = base64UrlDecode(parts[1]);
            byte[] ciphertextAndTag = base64UrlDecode(parts[2]);
            if (nonce.length != GCM_NONCE_BYTES || ciphertextAndTag.length < GCM_TAG_BITS / 8) {
                throw new IllegalArgumentException("암호문 길이 불일치");
            }
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return new String(cipher.doFinal(ciphertextAndTag), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 조회용 HMAC (64자 16진수)
     */
    public String hmac(String value) {
        if (value == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("조회용 해시를 만들지 못했습니다.", e);
        }
    }

    /**
     * 정규화 후 이메일 HMAC
     */
    public String emailHmac(String email) {
        return hmac(normalizeEmail(email));
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] decodeRequiredKey(String encodedKey, String propertyName) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(propertyName + " 값이 없습니다. 디스코드의 application.properties를 확인해 주세요.");
        }
        try {
            return Base64.getDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(propertyName + " 값이 Base64가 아닙니다.", e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
