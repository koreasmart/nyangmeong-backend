package com.nyangmeong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.TaskScheduler;

import com.nyangmeong.global.mail.MailSender;
import com.nyangmeong.global.notification.CareScheduleAlarmEvent;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.notification.domain.NotificationReceiveSetting;
import com.nyangmeong.notification.domain.NotificationRecipient;
import com.nyangmeong.notification.domain.NotificationSendingLog;
import com.nyangmeong.notification.mapper.NotificationMapper;
import com.nyangmeong.notification.mapper.NotificationSendingLogMapper;

/**
 * [검사 - 케어 일정 사전 알림 중복 발송 방지 (FN-AIM-006-15·17)]
 * - insert 를 UK_NOTI_SNDNG_LOG_1 (회원 + 유형 + 채널 + 시점 + 일정 + 일정일) 집합으로 흉내
 */
class CareScheduleAlarmDedupTest {

    private static final String EMAIL = "ljs_test@example.com";

    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final NotificationSendingLogMapper sendingLogMapper = mock(NotificationSendingLogMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final MailSender mailSender = mock(MailSender.class);
    private final CareScheduleAlarmService service = new CareScheduleAlarmService(notificationMapper,
            sendingLogMapper, notificationService, fieldCryptoService, mailSender, mock(TaskScheduler.class),
            "http://localhost:5173");

    /** 등록된 중복방지키 */
    private final Set<List<Object>> keys = new HashSet<>();

    @BeforeEach
    void setUp() {
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
        recipient.setMbrNcnm("진성");
        when(notificationMapper.selectRecipient(1L)).thenReturn(recipient);
        when(fieldCryptoService.decrypt("enc")).thenReturn(EMAIL);

        doAnswer(invocation -> {
            NotificationSendingLog sendingLog = invocation.getArgument(0);
            List<Object> key = List.of(sendingLog.getMbrSn(), sendingLog.getNotiTypeCd(),
                    sendingLog.getNotiChnlCd(), sendingLog.getBfhdNotiCd(), sendingLog.getSchdlSn(),
                    sendingLog.getSchdlYmd());
            if (!keys.add(key)) {
                throw new DuplicateKeyException("UK_NOTI_SNDNG_LOG_1");
            }
            sendingLog.setNotiSndngLogSn((long) keys.size());
            return null;
        }).when(sendingLogMapper).insert(any());
    }

    private CareScheduleAlarmEvent event(LocalDateTime schdlDt) {
        return new CareScheduleAlarmEvent(1L, 100L, schdlDt, true, "초코", "예방접종", "냥멍동물병원", 1);
    }

    @Test
    void 같은_이벤트를_두_번_받아도_메일_1회_인앱_1건이다() {
        CareScheduleAlarmEvent event = event(LocalDateTime.of(2026, 10, 22, 14, 30));

        service.send(event);
        service.send(event);

        verify(mailSender, times(1)).send(eq(EMAIL), anyString(), anyString());
        verify(notificationService, times(1)).create(any());
        assertThat(keys).hasSize(2);
    }

    @Test
    void 일정일이_바뀌면_새_건으로_다시_보낸다() {
        service.send(event(LocalDateTime.of(2026, 10, 22, 14, 30)));
        service.send(event(LocalDateTime.of(2026, 10, 23, 14, 30)));

        verify(mailSender, times(2)).send(eq(EMAIL), anyString(), anyString());
        verify(notificationService, times(2)).create(any());
        assertThat(keys).hasSize(4);
    }
}
