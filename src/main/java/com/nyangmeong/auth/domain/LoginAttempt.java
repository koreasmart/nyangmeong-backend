package com.nyangmeong.auth.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [로그인 시도 제한 VO]
 * - LGN_ATMPT_LMT 1:1, 아이디(HMAC) 하나당 한 행
 * - 조회는 mapper/auth/LoginAttemptMapper.xml
 */
@Data
public class LoginAttempt {
    private Long lgnAtmptLmtSn;
    private String lgnIdHmac;
    private Integer cntnuCertFailNmtm;
    private String scrtyCdHash;
    private String scrtyCdRqtYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
