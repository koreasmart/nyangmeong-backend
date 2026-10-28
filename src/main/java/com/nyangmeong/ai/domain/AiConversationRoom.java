package com.nyangmeong.ai.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [AI 대화방 VO]
 * - AI_CNVRS_ROOM 1:1, 제목은 암호문
 */
@Data
public class AiConversationRoom {
    /** AI대화방일련번호 */
    private Long aiCnvrsRoomSn;
    /** 회원일련번호 */
    private Long mbrSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 대화제목암호문 */
    private String cnvrsTtlEnc;
    /** 등록일시 */
    private LocalDateTime regDt;
}
