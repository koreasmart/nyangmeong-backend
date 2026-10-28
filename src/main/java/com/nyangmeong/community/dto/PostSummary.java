package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [게시글 목록 항목]
 * - 목록·최신 5건·오늘의 건강팁 공통, popular 는 인기글 배지
 */
@Data
public class PostSummary {
    private Long pstSn;
    private String bbsCd;
    private String pstTtl;
    private String pstCn;
    private String mbrNcnm;
    private LocalDateTime regDt;
    private Long thumbAtchFileSn;
    private Integer rcmdtnCnt;
    private Integer cmntCnt;
    private Boolean popular;
}
