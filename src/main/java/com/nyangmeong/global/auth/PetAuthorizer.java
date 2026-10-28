package com.nyangmeong.global.auth;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * [반려동물 권한 검사]
 * - 서비스 메서드 첫 줄에서 호출 (docs/convention/service.md)
 * - 양육자 = 주양육자(PCG) 또는 공동양육자(CCG), 연동 해제·삭제된 반려동물 제외
 */
@Component
@RequiredArgsConstructor
public class PetAuthorizer {

    private final PetAuthMapper petAuthMapper;

    /**
     * 양육자 권한 검사
     */
    public void checkCaregiver(Long mbrSn, Long petSn) {
        roleOf(mbrSn, petSn);
    }

    /**
     * 주양육자 권한 검사
     */
    public void checkPrimary(Long mbrSn, Long petSn) {
        if (!BrupRole.PRIMARY.is(roleOf(mbrSn, petSn))) {
            throw new CustomException(ErrorCode.PET_PRIMARY_ONLY);
        }
    }

    /**
     * 양육 역할 코드 조회
     */
    private String roleOf(Long mbrSn, Long petSn) {
        if (!petAuthMapper.existsPet(petSn)) {
            throw new CustomException(ErrorCode.PET_NOT_FOUND);
        }
        String brupRoleCd = petAuthMapper.selectBrupRoleCd(mbrSn, petSn);
        if (brupRoleCd == null) {
            throw new CustomException(ErrorCode.PET_ACCESS_DENIED);
        }
        return brupRoleCd;
    }
}
