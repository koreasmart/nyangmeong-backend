package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [게시글 상태 이력 VO]
 * - PST_STTS_HSTRY 1:1
 */
@Data
public class PostStatusHistory {
    private Long pstSttsHstrySn;
    private Long pstSn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
