package com.nyangmeong.global.security.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.security.captcha.CaptchaGenerator.Captcha;

/**
 * [검사 - 보안코드 생성]
 * - 영문 대문자·숫자 각 1개 이상, 6글자, PNG data URI
 */
class CaptchaGeneratorTest {

    private final CaptchaGenerator generator = new CaptchaGenerator();

    @Test
    void 코드는_6글자이고_영문_대문자와_숫자를_각각_포함한다() {
        for (int i = 0; i < 1000; i++) {
            String code = generator.newCode();

            assertThat(code).matches("[A-Z0-9]{6}").matches(".*[A-Z].*").matches(".*[0-9].*");
        }
    }

    @Test
    void 이미지는_PNG_data_URI이고_코드_원문을_담지_않는다() {
        Captcha captcha = generator.generate();

        assertThat(captcha.imageDataUri()).startsWith("data:image/png;base64,");
        byte[] png = Base64.getDecoder().decode(captcha.imageDataUri().substring("data:image/png;base64,".length()));
        assertThat(png).startsWith(new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        assertThat(captcha.imageDataUri()).doesNotContain(captcha.code());
    }
}
