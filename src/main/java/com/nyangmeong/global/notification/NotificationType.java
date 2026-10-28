package com.nyangmeong.global.notification;

import java.util.Arrays;
import java.util.List;

/**
 * [알림 유형 코드]
 * - AIMG02 (INAPP_NOTI.NOTI_TYPE_CD) -> 소속 탭
 */
public enum NotificationType {
    CARE_SCHEDULE("AIMC0008", NotificationTab.CARE),
    CARE_MATCH("AIMC0009", NotificationTab.COMMUNITY),
    COMMUNITY("AIMC0010", NotificationTab.COMMUNITY),
    CO_PARENTING("AIMC0011", NotificationTab.INFO),
    LOST_EXPIRY("AIMC0012", NotificationTab.COMMUNITY),
    EXPENSE("AIMC0013", NotificationTab.CARE),
    CHARACTER("AIMC0014", NotificationTab.INFO),
    NOTICE("AIMC0015", NotificationTab.INFO),
    INQUIRY_ANSWER("AIMC0030", NotificationTab.INFO);

    private final String code;
    private final NotificationTab tab;

    NotificationType(String code, NotificationTab tab) {
        this.code = code;
        this.tab = tab;
    }

    public String getCode() {
        return code;
    }

    public NotificationTab getTab() {
        return tab;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 유형 조회
     */
    public static NotificationType from(String code) {
        return Arrays.stream(values())
                .filter(type -> type.is(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 알림 유형: " + code));
    }

    /**
     * 탭에 묶인 유형 코드 목록
     */
    public static List<String> codesOf(NotificationTab tab) {
        return Arrays.stream(values())
                .filter(type -> type.tab == tab)
                .map(NotificationType::getCode)
                .toList();
    }
}
