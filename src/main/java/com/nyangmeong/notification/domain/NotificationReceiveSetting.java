package com.nyangmeong.notification.domain;

import lombok.Data;

/**
 * [알림 수신 설정 VO]
 * - MBR 의 알림 수신 컬럼만 (SCR-COM-008 설정 화면과 같은 컬럼)
 */
@Data
public class NotificationReceiveSetting {
    private Long mbrSn;
    private String mbrSttsCd;
    private Long mmExpndLimAmt;
    private String emlNotiRcptnYn;
    private String inappNotiRcptnYn;
    private String careSchdlNotiRcptnYn;
    private String mtchDlngNotiRcptnYn;
    private String cmntNotiRcptnYn;
    private String lostExpryNotiRcptnYn;
    private String expndLimNotiRcptnYn;
    private String charCrtNotiRcptnYn;
    private String thdyBfhdNotiYn;
    private String d1BfhdNotiYn;
    private String d3BfhdNotiYn;
    private String d7BfhdNotiYn;
}
