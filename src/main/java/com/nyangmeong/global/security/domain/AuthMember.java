package com.nyangmeong.global.security.domain;

import lombok.Data;

/**
 * [인증용 회원 VO]
 * - MBR 에서 인증에 필요한 컬럼만
 * - 조회는 mapper/global/AuthMemberMapper.xml
 */
@Data
public class AuthMember {
    private Long mbrSn;
    private String mbrSttsCd;
    private String mbrAuthrtCd;
    private String mbrNcnm;
    private Long atchFileSn;
    private String acntEmlEnc;
}
