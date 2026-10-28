package com.nyangmeong.global.mail;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * [가짜 메일 발송기]
 * - SMTP 계정 결정 전 개발용 : 보내지 않고 로그만
 * - 받는 주소는 로그에 남기지 않음 (docs/convention/common.md)
 * - 인증번호를 눈으로 확인하도록 본문은 debug 로만 남김 : 실제 발송기로 바꾸면 사라진다
 */
@Slf4j
@Component
public class LoggingMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String content) {
        log.info("[Mail] 가짜 발송 subject={}", subject);
        log.debug("[Mail] 가짜 발송 본문={}", content);
    }
}
