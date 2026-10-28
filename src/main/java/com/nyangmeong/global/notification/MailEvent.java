package com.nyangmeong.global.notification;

import java.util.Objects;

/**
 * [메일 발송 이벤트]
 * - EventPublisher.publish() 로만 발행
 * - 발송 리스너는 처음 쓰는 화면에서 추가
 */
public record MailEvent(Long mbrSn, NotificationType type, String mailTtl, String mailCn) {

    public MailEvent {
        Objects.requireNonNull(mbrSn, "mbrSn");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(mailTtl, "mailTtl");
    }
}
