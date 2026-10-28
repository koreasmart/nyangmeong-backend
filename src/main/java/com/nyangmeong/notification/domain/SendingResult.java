package com.nyangmeong.notification.domain;

/**
 * [발송 결과 코드]
 * - AIMG04 (NOTI_SNDNG_LOG.SNDNG_RSLT_CD)
 */
public enum SendingResult {
    SUCCESS("AIMC0018"),
    FAIL("AIMC0019");

    private final String code;

    SendingResult(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
