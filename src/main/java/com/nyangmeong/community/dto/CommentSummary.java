package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [댓글 목록 항목]
 * - 삭제된 댓글은 delYn=true, 내용·작성자 없음 (답글은 유지)
 */
@Data
public class CommentSummary {
    private Long cmntSn;
    private Long prntCmntSn;
    private boolean delYn;
    private String cmntCn;
    private Long mbrSn;
    private String mbrNcnm;
    private boolean sysAutoCrtYn;
    private Boolean owner;
    private LocalDateTime regDt;
}
