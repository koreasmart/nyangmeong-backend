package com.nyangmeong.notification.domain;

import lombok.Data;

/**
 * [알림 템플릿 VO]
 * - NOTI_TMPLT 1:1 (알림 유형 + 채널당 1행, 관리자 등록 REQ-ADM-011)
 */
@Data
public class NotificationTemplate {
    private Long notiTmpltSn;
    private String notiTypeCd;
    private String notiChnlCd;
    private String notiTtl;
    private String notiCn;
    private String useYn;
}
