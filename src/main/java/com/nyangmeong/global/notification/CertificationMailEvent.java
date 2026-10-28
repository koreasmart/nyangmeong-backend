package com.nyangmeong.global.notification;

import java.util.Objects;

/**
 * [인증번호 메일 발송 이벤트]
 * - 가입 전·비밀번호 찾기처럼 회원번호가 없는 메일 : MailEvent 대신 받는 주소를 직접 담는다
 * - EventPublisher.publish() 로만 발행, 받는 주소는 로그에 남기지 않는다
 */
public record CertificationMailEvent(String to, String mailTtl, String mailCn) {

    public CertificationMailEvent {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(mailTtl, "mailTtl");
        Objects.requireNonNull(mailCn, "mailCn");
    }
}
