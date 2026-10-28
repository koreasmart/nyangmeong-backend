package com.nyangmeong.global.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - 작성자 권한]
 */
class OwnerAuthorizerTest {

    private final OwnerAuthorizer ownerAuthorizer = new OwnerAuthorizer();

    @Test
    void 본인이면_통과한다() {
        assertThatCode(() -> ownerAuthorizer.checkOwner(1L, 1L)).doesNotThrowAnyException();
    }

    @Test
    void 남이면_NOT_OWNER() {
        assertThatThrownBy(() -> ownerAuthorizer.checkOwner(1L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_OWNER);
    }

    @Test
    void 작성자가_없으면_NOT_OWNER() {
        assertThatThrownBy(() -> ownerAuthorizer.checkOwner(1L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_OWNER);
    }
}
