package com.nyangmeong.notification.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [케어 일정 사전 알림 시점]
 * - MBR 의 THDY·D1·D3·D7_BFHD_NOTI_YN 컬럼과 1:1 (FN-COM-008-06)
 * - 코드 = AIMG05 (NOTI_SNDNG_LOG.BFHD_NOTI_CD)
 */
public enum NotificationLeadTime {
    TODAY("AIMC0020", 0),
    D1("AIMC0021", 1),
    D3("AIMC0022", 3),
    D7("AIMC0023", 7);

    private final String code;
    private final int days;

    NotificationLeadTime(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public String getCode() {
        return code;
    }

    public int getDays() {
        return days;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 남은 일수로 시점 조회
     */
    public static Optional<NotificationLeadTime> fromDays(int days) {
        return Arrays.stream(values())
                .filter(leadTime -> leadTime.days == days)
                .findFirst();
    }

    /**
     * 회원 설정의 시점 선택 여부
     */
    public boolean isSelected(NotificationReceiveSetting setting) {
        String yn = switch (this) {
            case TODAY -> setting.getThdyBfhdNotiYn();
            case D1 -> setting.getD1BfhdNotiYn();
            case D3 -> setting.getD3BfhdNotiYn();
            case D7 -> setting.getD7BfhdNotiYn();
        };
        return "Y".equals(yn);
    }
}
