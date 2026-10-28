package com.nyangmeong.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.TaskScheduler;

import com.nyangmeong.global.mail.MailSender;
import com.nyangmeong.global.notification.CareScheduleAlarmEvent;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.notification.domain.NotificationReceiveSetting;
import com.nyangmeong.notification.domain.NotificationRecipient;
import com.nyangmeong.notification.mapper.NotificationMapper;
import com.nyangmeong.notification.mapper.NotificationSendingLogMapper;

/**
 * [검사 - 발송 이력 선점의 중복 외 DB 오류]
 * - 외래키 위반 등은 "이미 보냄"으로 삼키지 않고 호출자(리스너)로 전달
 */
class CareScheduleAlarmClaimErrorTest {

    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final NotificationSendingLogMapper sendingLogMapper = mock(NotificationSendingLogMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final MailSender mailSender = mock(MailSender.class);
    private final CareScheduleAlarmService service = new CareScheduleAlarmService(notificationMapper,
            sendingLogMapper, notificationService, fieldCryptoService, mailSender, mock(TaskScheduler.class),
            "http://localhost:5173");

    @Test
    void 외래키_위반은_건너뛰지_않고_던진다() {
        NotificationReceiveSetting setting = new NotificationReceiveSetting();
        setting.setMbrSn(1L);
        setting.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        setting.setEmlNotiRcptnYn("Y");
        setting.setInappNotiRcptnYn("Y");
        setting.setCareSchdlNotiRcptnYn("Y");
        setting.setD1BfhdNotiYn("Y");
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting);
        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setMbrSn(1L);
        recipient.setAcntEmlEnc("enc");
        when(notificationMapper.selectRecipient(1L)).thenReturn(recipient);
        doThrow(new DataIntegrityViolationException("FK_NOTI_SNDNG_LOG_SCHDL")).when(sendingLogMapper).insert(any());

        CareScheduleAlarmEvent event = new CareScheduleAlarmEvent(1L, 999L, LocalDateTime.of(2026, 10, 22, 14, 30),
                true, "초코", "예방접종", "냥멍동물병원", 1);

        assertThatThrownBy(() -> service.send(event)).isInstanceOf(DataIntegrityViolationException.class);
        verify(notificationService, never()).create(any());
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        verify(sendingLogMapper, never()).updateResult(any(), anyString(), anyInt(), any(), anyString());
    }
}
