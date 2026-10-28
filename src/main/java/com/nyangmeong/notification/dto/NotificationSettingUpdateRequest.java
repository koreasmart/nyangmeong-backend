package com.nyangmeong.notification.dto;

import java.util.List;

import com.nyangmeong.notification.domain.NotificationLeadTime;

import jakarta.validation.constraints.NotNull;

/**
 * [알림 설정 저장 요청]
 * - 이메일 주소·한도 금액·항상 받는 알림은 받지 않음 (FN-COM-008-03·09·11·12)
 * - leadTimes 중복 값은 하나로 본다
 */
public record NotificationSettingUpdateRequest(@NotNull Boolean emlNotiRcptnYn, @NotNull Boolean inappNotiRcptnYn,
        @NotNull Boolean careSchdlNotiRcptnYn, @NotNull List<@NotNull NotificationLeadTime> leadTimes,
        @NotNull Boolean mtchDlngNotiRcptnYn, @NotNull Boolean cmntNotiRcptnYn, @NotNull Boolean lostExpryNotiRcptnYn,
        @NotNull Boolean expndLimNotiRcptnYn) {
}
