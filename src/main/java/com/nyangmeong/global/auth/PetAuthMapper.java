package com.nyangmeong.global.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * [반려동물 권한 Mapper]
 * - global 전용 Mapper (pet 도메인 Mapper 미사용)
 */
@Mapper
public interface PetAuthMapper {

    /**
     * 삭제되지 않은 반려동물 존재 여부
     */
    boolean existsPet(Long petSn);

    /**
     * 회원의 유효한 양육 역할 코드 조회
     */
    String selectBrupRoleCd(@Param("mbrSn") Long mbrSn, @Param("petSn") Long petSn);
}
