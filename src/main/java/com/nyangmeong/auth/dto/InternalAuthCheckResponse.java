package com.nyangmeong.auth.dto;

import com.nyangmeong.global.security.domain.LoginMember;

/**
 * [내부 인증 확인 응답]
 * - Python 미들웨어가 /ai/** 요청마다 받는 회원 번호와 권한
 */
public record InternalAuthCheckResponse(Long mbrSn, String mbrAuthrtCd) {

    public static InternalAuthCheckResponse of(LoginMember member) {
        return new InternalAuthCheckResponse(member.id(), member.mbrAuthrtCd());
    }
}
