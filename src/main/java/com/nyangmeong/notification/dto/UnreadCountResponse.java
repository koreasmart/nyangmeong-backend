package com.nyangmeong.notification.dto;

/**
 * [미읽음 알림 개수 응답]
 * - 99+ 표시는 프론트
 */
public record UnreadCountResponse(long count) {
}
