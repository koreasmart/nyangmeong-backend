package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [댓글 VO]
 * - CMNT 1:1
 */
@Data
public class Comment {
    private Long cmntSn;
    private Long prntCmntSn;
    private Long pstSn;
    private Long mbrSn;
    private String cmntCn;
    private String sysAutoCrtYn;
    private String delYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
