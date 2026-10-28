package com.nyangmeong.global.auth;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [작성자 권한 검사]
 * - 대상 조회 직후 호출 (docs/convention/service.md)
 */
@Component
public class OwnerAuthorizer {

    /**
     * 작성자 본인 검사
     */
    public void checkOwner(Long mbrSn, Long ownerMbrSn) {
        if (mbrSn == null || !Objects.equals(mbrSn, ownerMbrSn)) {
            throw new CustomException(ErrorCode.NOT_OWNER);
        }
    }
}
