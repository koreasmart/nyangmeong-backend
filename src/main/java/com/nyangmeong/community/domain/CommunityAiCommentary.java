package com.nyangmeong.community.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [커뮤니티 AI 코멘트 VO]
 * - CMNTY_AI_CMTRY 1:1
 */
@Data
public class CommunityAiCommentary {
    private Long cmntyAiCmtrySn;
    private String cmtrySrcCd;
    private LocalDate cmtryCrtrYmd;
    private String cmtryCn;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
