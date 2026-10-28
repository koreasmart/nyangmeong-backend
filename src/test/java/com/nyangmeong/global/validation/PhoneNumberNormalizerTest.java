package com.nyangmeong.global.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.nyangmeong.global.error.BaseException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - 휴대전화번호 정규화]
 * - 공백·하이픈·+82 표기가 같은 번호로 통일되는지, 형식이 아닌 값은 거절되는지
 */
class PhoneNumberNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {"01012345678", "010-1234-5678", " 010 1234 5678 ", "+82 10 1234 5678",
            "+82-10-1234-5678", "+8201012345678"})
    void 같은_번호의_여러_표기는_국내_표기로_통일된다(String raw) {
        assertThat(PhoneNumberNormalizer.normalize(raw)).isEqualTo("01012345678");
    }

    @Test
    void 구형_10자리_번호도_받는다() {
        assertThat(PhoneNumberNormalizer.normalize("011-234-5678")).isEqualTo("0112345678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0212345678", "010123456", "0101234567890", "abc", "010-12a4-5678", "8210 1234 5678",
            "+1 415 555 0100"})
    void 휴대전화번호가_아니면_거절한다(String raw) {
        assertThatThrownBy(() -> PhoneNumberNormalizer.normalize(raw))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PHONE_NUMBER));
    }

    @Test
    void null도_거절한다() {
        assertThatThrownBy(() -> PhoneNumberNormalizer.normalize(null))
                .isInstanceOfSatisfying(BaseException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_PHONE_NUMBER));
    }
}
