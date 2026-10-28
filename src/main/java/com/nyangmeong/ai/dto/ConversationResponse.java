package com.nyangmeong.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [대화 상세 응답]
 * - messages 는 msgSeq 오름차순
 */
public record ConversationResponse(Long aiCnvrsRoomSn, Long petSn, String cnvrsTtl, LocalDateTime regDt,
        List<ConversationMessageResponse> messages) {
}
