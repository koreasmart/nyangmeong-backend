package com.nyangmeong.notification.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nyangmeong.global.notification.CareScheduleAlarmEvent;
import com.nyangmeong.global.notification.NotificationEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [알림 이벤트 리스너]
 * - 발행한 트랜잭션 커밋 뒤 이벤트마다 새 트랜잭션으로 저장·발송
 * - 저장 실패가 이미 커밋된 본 작업의 호출자에게 번지지 않게 예외는 로그만
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final CareScheduleAlarmService careScheduleAlarmService;

    /**
     * 인앱 알림 이벤트 수신
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationEvent event) {
        try {
            notificationService.create(event);
        } catch (RuntimeException e) {
            log.error("[Notification] 저장 실패 mbrSn={} type={}", event.mbrSn(), event.type(), e);
        }
    }

    /**
     * 케어 일정 사전 알림 이벤트 수신
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(CareScheduleAlarmEvent event) {
        try {
            careScheduleAlarmService.send(event);
        } catch (RuntimeException e) {
            log.error("[CareScheduleAlarm] 발송 실패 mbrSn={} schdlSn={}", event.mbrSn(), event.schdlSn(), e);
        }
    }
}
