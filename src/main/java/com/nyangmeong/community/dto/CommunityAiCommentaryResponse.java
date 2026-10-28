package com.nyangmeong.community.dto;

import java.time.LocalDate;

/**
 * [오늘의 이야기 응답]
 * - 해당 일자 코멘트가 없으면 cmntyAiCmtrySn 없이 기본 문구
 */
public record CommunityAiCommentaryResponse(Long cmntyAiCmtrySn, LocalDate cmtryCrtrYmd, String cmtryCn) {
}
