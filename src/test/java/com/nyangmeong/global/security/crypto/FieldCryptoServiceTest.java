package com.nyangmeong.global.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - 개인정보 필드 암복호화]
 */
class FieldCryptoServiceTest {

    private static final String AES_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String HMAC_KEY = Base64.getEncoder().encodeToString("hmac-key-for-tests-32-bytes-long".getBytes());

    private FieldCryptoService service;

    @BeforeEach
    void setUp() {
        service = create(AES_KEY, HMAC_KEY);
    }

    private static FieldCryptoService create(String aesKey, String hmacKey) {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKeyBase64(aesKey);
        properties.setHmacKeyBase64(hmacKey);
        FieldCryptoService created = new FieldCryptoService(properties);
        created.initializeKeys();
        return created;
    }

    @Test
    void 암호화한_값을_복호화하면_원문이_된다() {
        String encrypted = service.encrypt("jinseong@gmail.com");

        assertThat(encrypted).startsWith("v1.");
        assertThat(encrypted.split("\\.")).hasSize(3);
        assertThat(service.decrypt(encrypted)).isEqualTo("jinseong@gmail.com");
    }

    @Test
    void 같은_원문도_암호문은_매번_다르다() {
        assertThat(service.encrypt("010-1234-5678")).isNotEqualTo(service.encrypt("010-1234-5678"));
    }

    @Test
    void hmac은_같은_입력에_같은_64자_16진수다() {
        String first = service.hmac("jinseong@gmail.com");

        assertThat(first).hasSize(64).matches("[0-9a-f]+");
        assertThat(service.hmac("jinseong@gmail.com")).isEqualTo(first);
        assertThat(service.hmac("other@gmail.com")).isNotEqualTo(first);
    }

    @Test
    void emailHmac은_정규화한_뒤_만든다() {
        assertThat(service.emailHmac(" JinSeong@Gmail.com ")).isEqualTo(service.hmac("jinseong@gmail.com"));
    }

    @Test
    void normalizeEmail은_공백을_지우고_소문자로_바꾼다() {
        assertThat(service.normalizeEmail(" A@B.com ")).isEqualTo("a@b.com");
        assertThat(service.normalizeEmail(null)).isNull();
    }

    @Test
    void 형식이_다르거나_키가_다른_암호문은_500이다() {
        String encrypted = service.encrypt("secret");
        FieldCryptoService otherKey = create(Base64.getEncoder().encodeToString("another-32-byte-aes-key-000000!!".getBytes()),
                HMAC_KEY);

        assertThatThrownBy(() -> service.decrypt("plain-text"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        assertThatThrownBy(() -> otherKey.decrypt(encrypted)).isInstanceOf(CustomException.class);
    }

    @Test
    void null은_null이다() {
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
        assertThat(service.hmac(null)).isNull();
    }

    @Test
    void 키가_비었거나_길이가_틀리면_기동_때_실패한다() {
        assertThatThrownBy(() -> create("", HMAC_KEY)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> create(Base64.getEncoder().encodeToString(new byte[16]), HMAC_KEY))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> create(AES_KEY, Base64.getEncoder().encodeToString(new byte[8])))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> create("not base64!", HMAC_KEY)).isInstanceOf(IllegalStateException.class);
    }
}
