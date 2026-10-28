package com.nyangmeong.member.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [이메일 인증 VO]
 * - EML_CERT 1:1 중 가입 인증에 쓰는 컬럼
 */
@Data
public class EmailCertification {
    private Long emlCertSn;
    private String emlCertPrpsCd;
    private String certTrgtEmlEnc;
    private String certTrgtEmlHmac;
    private String certNoHash;
    private Integer certFailNmtm;
    private LocalDateTime certExpryDt;
    private LocalDateTime certCmptnDt;
    private LocalDateTime certUseCmptnDt;
    private LocalDateTime certInvldDt;
    private LocalDateTime regDt;
}
