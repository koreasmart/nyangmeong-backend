package com.nyangmeong.global.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [검사 - 트랜잭션 안에서만 이벤트 발행]
 */
class EventPublisherTest {

    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final EventPublisher eventPublisher = new EventPublisher(applicationEventPublisher);

    private final NotificationEvent notificationEvent = new NotificationEvent(1L, NotificationType.NOTICE,
            "9/20 서버 점검 안내", null, "/support/notices");
    private final MailEvent mailEvent = new MailEvent(1L, NotificationType.CARE_SCHEDULE, "케어 일정 안내", "본문");

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @Test
    void 트랜잭션_안에서는_알림_이벤트가_나간다() {
        beginTransaction();

        eventPublisher.publish(notificationEvent);

        verify(applicationEventPublisher).publishEvent(notificationEvent);
    }

    @Test
    void 트랜잭션_안에서는_메일_이벤트가_나간다() {
        beginTransaction();

        eventPublisher.publish(mailEvent);

        verify(applicationEventPublisher).publishEvent(mailEvent);
    }

    @Test
    void 트랜잭션_밖에서_알림_이벤트를_발행하면_예외가_난다() {
        assertThatThrownBy(() -> eventPublisher.publish(notificationEvent))
                .isInstanceOf(IllegalStateException.class);
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void 트랜잭션_밖에서_메일_이벤트를_발행하면_예외가_난다() {
        assertThatThrownBy(() -> eventPublisher.publish(mailEvent))
                .isInstanceOf(IllegalStateException.class);
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }
}
