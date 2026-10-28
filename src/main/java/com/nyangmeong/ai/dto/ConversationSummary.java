package com.nyangmeong.ai.dto;

import java.time.LocalDateTime;

/**
 * [대화 목록 응답]
 * - cnvrsTtl 은 복호화한 제목
 */
public record ConversationSummary(Long aiCnvrsRoomSn, Long petSn, String cnvrsTtl, LocalDateTime regDt) {
}
