package com.nyangmeong.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nyangmeong.global.security.domain.AuthMember;

/**
 * [내 정보 응답]
 * - atchFileSn 없으면 JSON 에서 제외
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthMeResponse(Long mbrSn, String mbrAuthrtCd, String mbrNcnm, Long atchFileSn,
        String maskedAcntEml) {

    public static AuthMeResponse of(AuthMember member, String maskedAcntEml) {
        return new AuthMeResponse(member.getMbrSn(), member.getMbrAuthrtCd(), member.getMbrNcnm(),
                member.getAtchFileSn(), maskedAcntEml);
    }
}
