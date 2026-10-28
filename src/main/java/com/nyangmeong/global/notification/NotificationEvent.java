package com.nyangmeong.global.notification;

import java.util.Objects;

/**
 * [인앱 알림 발행 이벤트]
 * - EventPublisher.publish() 로만 발행 -> notification 도메인이 커밋 뒤 INAPP_NOTI 저장
 * - 수신 OFF 유형은 저장 단계에서 미생성
 */
public record NotificationEvent(Long mbrSn, NotificationType type, String notiTtl, String notiCn,
        String mvmnPath) {

    public NotificationEvent {
        Objects.requireNonNull(mbrSn, "mbrSn");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(notiTtl, "notiTtl");
    }
}
