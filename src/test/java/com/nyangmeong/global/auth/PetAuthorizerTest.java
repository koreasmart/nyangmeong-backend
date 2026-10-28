package com.nyangmeong.global.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - 반려동물 권한]
 */
class PetAuthorizerTest {

    private final PetAuthMapper petAuthMapper = mock(PetAuthMapper.class);
    private final PetAuthorizer petAuthorizer = new PetAuthorizer(petAuthMapper);

    @Test
    void 주양육자와_공동양육자는_양육자_권한이_있다() {
        when(petAuthMapper.existsPet(10L)).thenReturn(true);
        when(petAuthMapper.selectBrupRoleCd(1L, 10L)).thenReturn(BrupRole.PRIMARY.getCode());
        when(petAuthMapper.selectBrupRoleCd(2L, 10L)).thenReturn(BrupRole.CO_PARENT.getCode());

        assertThatCode(() -> petAuthorizer.checkCaregiver(1L, 10L)).doesNotThrowAnyException();
        assertThatCode(() -> petAuthorizer.checkCaregiver(2L, 10L)).doesNotThrowAnyException();
    }

    @Test
    void 없는_반려동물은_PET_NOT_FOUND() {
        when(petAuthMapper.existsPet(10L)).thenReturn(false);

        assertThatThrownBy(() -> petAuthorizer.checkCaregiver(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PET_NOT_FOUND);
    }

    @Test
    void 양육관계가_없으면_PET_ACCESS_DENIED() {
        when(petAuthMapper.existsPet(10L)).thenReturn(true);

        assertThatThrownBy(() -> petAuthorizer.checkPrimary(3L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PET_ACCESS_DENIED);
    }

    @Test
    void 공동양육자는_주양육자_권한이_없다() {
        when(petAuthMapper.existsPet(10L)).thenReturn(true);
        when(petAuthMapper.selectBrupRoleCd(2L, 10L)).thenReturn(BrupRole.CO_PARENT.getCode());

        assertThatThrownBy(() -> petAuthorizer.checkPrimary(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
    }
}
