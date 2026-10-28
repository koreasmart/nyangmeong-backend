package com.nyangmeong.ai.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [AI 대화 메시지 VO]
 * - AI_CNVRS_MSG 1:1, 내용은 암호문
 */
@Data
public class AiConversationMessage {
    /** AI대화메시지일련번호 */
    private Long aiCnvrsMsgSn;
    /** AI대화방일련번호 */
    private Long aiCnvrsRoomSn;
    /** 역할공통코드(AIMG06) */
    private String roleCd;
    /** 대화내용암호문 */
    private String cnvrsCnEnc;
    /** 메시지순서 */
    private Integer msgSeq;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 완료여부 */
    private String cmptnYn;
}
