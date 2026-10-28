package com.nyangmeong.ai.dto;

import java.time.LocalDateTime;

/**
 * [대화 메시지 응답]
 * - cnvrsCn 은 복호화한 내용
 */
public record ConversationMessageResponse(Long aiCnvrsMsgSn, String roleCd, String cnvrsCn, Integer msgSeq,
        boolean cmptnYn, LocalDateTime regDt) {
}
