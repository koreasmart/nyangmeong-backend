package com.nyangmeong.notification.domain;

import lombok.Data;

/**
 * [알림 수신자 VO]
 * - MBR 의 메일 주소·치환용 닉네임 컬럼만
 */
@Data
public class NotificationRecipient {
    private Long mbrSn;
    private String acntEmlEnc;
    private String mbrNcnm;
}
