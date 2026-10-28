package com.nyangmeong.notification.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [알림 드롭다운 목록 행]
 * - tab 은 유형 코드로 서비스가 채움, 이동 분기는 프론트
 */
@Data
public class NotificationSummary {
    private Long inappNotiSn;
    private String notiTypeCd;
    private String tab;
    private String notiTtl;
    private String notiCn;
    private String mvmnPath;
    private LocalDateTime prslDt;
    private LocalDateTime regDt;
}
