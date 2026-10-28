package com.nyangmeong.notification.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [알림 발송 이력 VO]
 * - NOTI_SNDNG_LOG 1:1 (중복방지키 DPCN_PRVN_KEY 는 DB 생성 컬럼)
 */
@Data
public class NotificationSendingLog {
    private Long notiSndngLogSn;
    private Long mbrSn;
    private Long schdlSn;
    private Long lostTpofSn;
    private String notiTypeCd;
    private String notiChnlCd;
    private String sndngRsltCd;
    private String bfhdNotiCd;
    private Integer rtryNmtm;
    private String failRsn;
    private String notiCrtrYm;
    private LocalDateTime sndngDt;
    private LocalDate schdlYmd;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String rgtrId;
    private String mdfrId;
}
