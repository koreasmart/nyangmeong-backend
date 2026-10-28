package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [게시글 이미지 VO]
 * - PST_IMG 1:1
 */
@Data
public class PostImage {
    private Long pstImgSn;
    private Long pstSn;
    private Long atchFileSn;
    private Integer sortSeq;
    private String delYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String rgtrId;
    private String mdfrId;
}
