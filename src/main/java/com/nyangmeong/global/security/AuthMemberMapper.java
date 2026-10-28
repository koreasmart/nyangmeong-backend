package com.nyangmeong.global.security;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.global.security.domain.AuthMember;

/**
 * [인증용 회원 Mapper]
 * - global 전용 Mapper (member 도메인 Mapper 미사용)
 */
@Mapper
public interface AuthMemberMapper {

    /**
     * 토큰 검증용 회원 조회 (상태/권한)
     */
    AuthMember selectForAuth(Long mbrSn);

    /**
     * 유효한 로그인 세션의 회원 조회 (상태/권한)
     */
    AuthMember selectForSession(@Param("mbrSn") Long mbrSn, @Param("rfrshTknSn") Long rfrshTknSn);

    /**
     * /auth/me 용 회원 조회
     */
    AuthMember selectForMe(Long mbrSn);
}
