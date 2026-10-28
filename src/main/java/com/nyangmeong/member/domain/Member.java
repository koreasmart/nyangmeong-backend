package com.nyangmeong.member.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [회원 VO]
 * - MBR 1:1 중 로그인·가입·닉네임에 쓰는 컬럼만
 * - 조회는 mapper/member/MemberMapper.xml
 */
@Data
public class Member {
    private Long mbrSn;
    private Long atchFileSn;
    private String mbrSttsCd;
    private String mbrAuthrtCd;
    private String joinPathCd;
    private String acntEmlEnc;
    private String acntEmlHmac;
    private String mblTelnoEnc;
    private String mblTelnoHmac;
    private String pswdHash;
    private String mbrNcnm;
    private LocalDateTime lastLgnDt;
    private String ncnmStngCmptnYn;
}
