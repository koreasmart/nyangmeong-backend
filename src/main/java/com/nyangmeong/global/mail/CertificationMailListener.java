package com.nyangmeong.global.mail;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nyangmeong.global.notification.CertificationMailEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [인증번호 메일 리스너]
 * - 발행한 트랜잭션이 커밋된 뒤에만 발송
 * - 발송 실패가 이미 커밋된 본 작업으로 번지지 않게 예외는 로그만 (실패 안내는 SMTP 결정 뒤)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificationMailListener {

    private final MailSender mailSender;

    /**
     * 인증번호 메일 이벤트 수신
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CertificationMailEvent event) {
        try {
            mailSender.send(event.to(), event.mailTtl(), event.mailCn());
        } catch (RuntimeException e) {
            log.error("[Mail] 인증번호 메일 발송 실패 subject={}", event.mailTtl(), e);
        }
    }
}
