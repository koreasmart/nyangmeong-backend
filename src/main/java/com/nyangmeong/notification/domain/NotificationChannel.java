package com.nyangmeong.notification.domain;

/**
 * [알림 채널 코드]
 * - AIMG03 (NOTI_TMPLT·NOTI_SNDNG_LOG.NOTI_CHNL_CD)
 */
public enum NotificationChannel {
    EMAIL("AIMC0016"),
    INAPP("AIMC0017");

    private final String code;

    NotificationChannel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
