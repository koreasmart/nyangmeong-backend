package com.nyangmeong.global.security.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [Refresh 토큰 VO]
 * - RFRSH_TKN 1:1, 행 하나 = 로그인 세션 하나
 * - 조회는 mapper/global/RefreshTokenMapper.xml
 */
@Data
public class RefreshToken {
    private Long rfrshTknSn;
    private Long mbrSn;
    private String rfrshTknHash;
    private LocalDateTime expryDt;
    private LocalDateTime dscdDt;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
