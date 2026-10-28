package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [게시글 VO]
 * - PST 1:1
 */
@Data
public class Post {
    private Long pstSn;
    private Long mbrSn;
    private String bbsCd;
    private String pstSttsCd;
    private String pstTtl;
    private String pstCn;
    private Integer inqCnt;
    private Integer rcmdtnCnt;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String mdfrId;
    private Integer hlthInfoExpsrSeq;
}
