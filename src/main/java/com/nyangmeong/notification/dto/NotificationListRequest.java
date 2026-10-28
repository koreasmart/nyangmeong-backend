package com.nyangmeong.notification.dto;

import com.nyangmeong.global.notification.NotificationTab;

/**
 * [알림 목록 조회 조건]
 * - tab 생략 -> ALL
 */
public record NotificationListRequest(NotificationTab tab) {
}
