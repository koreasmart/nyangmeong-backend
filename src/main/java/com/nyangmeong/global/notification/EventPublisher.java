package com.nyangmeong.global.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;

/**
 * [알림·메일 이벤트 발행기]
 * - 트랜잭션 밖 발행 -> IllegalStateException
 * - 리스너가 커밋 뒤에 받으므로 트랜잭션 없이 발행하면 이벤트가 오류 없이 사라짐 (docs/convention/service.md)
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 인앱 알림 이벤트 발행
     */
    public void publish(NotificationEvent event) {
        checkTransaction();
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 메일 이벤트 발행
     */
    public void publish(MailEvent event) {
        checkTransaction();
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 인증번호 메일 이벤트 발행
     */
    public void publish(CertificationMailEvent event) {
        checkTransaction();
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 케어 일정 사전 알림 이벤트 발행
     */
    public void publish(CareScheduleAlarmEvent event) {
        checkTransaction();
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 트랜잭션 확인
     */
    private void checkTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("EventPublisher.publish() 는 @Transactional 메서드 안에서만 호출할 수 있습니다.");
        }
    }
}
