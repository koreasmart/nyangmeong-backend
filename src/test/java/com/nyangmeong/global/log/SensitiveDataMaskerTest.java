package com.nyangmeong.global.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 민감정보 마스킹]
 */
class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void 이메일은_첫_글자만_남긴다() {
        assertThat(masker.maskText("보낸 사람 jinseong@gmail.com 확인")).isEqualTo("보낸 사람 j***@gmail.com 확인");
    }

    @Test
    void 휴대전화는_가운데를_가린다() {
        assertThat(masker.maskText("010-1234-5678")).isEqualTo("010-****-5678");
        assertThat(masker.maskText("01012345678")).isEqualTo("010-****-5678");
        assertThat(masker.maskText("+82 10-1234-5678")).isEqualTo("010-****-5678");
    }

    @Test
    void URL_인코딩된_값도_풀어서_가린다() {
        assertThat(masker.maskUri("/api/members?acntEml=a%40b.com")).isEqualTo("/api/members?acntEml=a***@b.com");
    }

    @Test
    void null과_민감정보_없는_문자열은_그대로다() {
        assertThat(masker.maskText(null)).isNull();
        assertThat(masker.maskUri("/api/pets/3")).isEqualTo("/api/pets/3");
    }
}
