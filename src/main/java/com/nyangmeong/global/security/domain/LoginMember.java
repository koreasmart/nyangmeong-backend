package com.nyangmeong.global.security.domain;

/**
 * [로그인 회원]
 * - @AuthenticationPrincipal 로 받는 인증 주체
 * - id : MBR_SN / mbrAuthrtCd : USRG02 코드
 */
public record LoginMember(Long id, String mbrAuthrtCd) {

    public boolean isAdmin() {
        return MemberAuthority.ADMIN.is(mbrAuthrtCd);
    }
}
