package com.nyangmeong.notification.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [인앱 알림 VO]
 * - INAPP_NOTI 1:1
 */
@Data
public class InappNotification {
    private Long inappNotiSn;
    private Long mbrSn;
    private String notiTypeCd;
    private String notiTtl;
    private String notiCn;
    private String mvmnPath;
    private LocalDateTime prslDt;
    private LocalDateTime autoHideDt;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
