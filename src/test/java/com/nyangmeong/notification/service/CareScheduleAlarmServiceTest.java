package com.nyangmeong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.TaskScheduler;

import com.nyangmeong.global.mail.MailSender;
import com.nyangmeong.global.notification.CareScheduleAlarmEvent;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.notification.domain.NotificationChannel;
import com.nyangmeong.notification.domain.NotificationLeadTime;
import com.nyangmeong.notification.domain.NotificationReceiveSetting;
import com.nyangmeong.notification.domain.NotificationRecipient;
import com.nyangmeong.notification.domain.NotificationSendingLog;
import com.nyangmeong.notification.domain.NotificationTemplate;
import com.nyangmeong.notification.domain.SendingResult;
import com.nyangmeong.notification.mapper.NotificationMapper;
import com.nyangmeong.notification.mapper.NotificationSendingLogMapper;

/**
 * [검사 - 케어 일정 사전 알림 판정·이력·발송·재시도]
 */
class CareScheduleAlarmServiceTest {

    private static final String FRONT = "http://localhost:5173";
    private static final String EMAIL = "ljs_test@example.com";

    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final NotificationSendingLogMapper sendingLogMapper = mock(NotificationSendingLogMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final MailSender mailSender = mock(MailSender.class);
    private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
    private final CareScheduleAlarmService service = new CareScheduleAlarmService(notificationMapper,
            sendingLogMapper, notificationService, fieldCryptoService, mailSender, taskScheduler, FRONT);

    /** 선점된 이력 (등록 순서대로) */
    private final List<NotificationSendingLog> claimed = new ArrayList<>();

    @BeforeEach
    void setUp() {
        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setMbrSn(1L);
        recipient.setAcntEmlEnc("enc");
        recipient.setMbrNcnm("진성");
        when(notificationMapper.selectRecipient(1L)).thenReturn(recipient);
        when(fieldCryptoService.decrypt("enc")).thenReturn(EMAIL);
        doAnswer(invocation -> {
            NotificationSendingLog sendingLog = invocation.getArgument(0);
            sendingLog.setNotiSndngLogSn(500L + claimed.size());
            claimed.add(sendingLog);
            return null;
        }).when(sendingLogMapper).insert(any());
    }

    private NotificationReceiveSetting setting(String emlYn, String inappYn) {
        NotificationReceiveSetting setting = new NotificationReceiveSetting();
        setting.setMbrSn(1L);
        setting.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        setting.setEmlNotiRcptnYn(emlYn);
        setting.setInappNotiRcptnYn(inappYn);
        setting.setCareSchdlNotiRcptnYn("Y");
        setting.setThdyBfhdNotiYn("Y");
        setting.setD1BfhdNotiYn("Y");
        setting.setD3BfhdNotiYn("N");
        setting.setD7BfhdNotiYn("N");
        return setting;
    }

    private CareScheduleAlarmEvent event(int bfhdDayCnt, boolean hrInptYn) {
        return new CareScheduleAlarmEvent(1L, 100L, LocalDateTime.of(2026, 10, 22, 14, 30), hrInptYn, "초코",
                "예방접종", "냥멍동물병원", bfhdDayCnt);
    }

    /**
     * 예약된 재시도 수집
     */
    private List<Runnable> collectRetries() {
        List<Runnable> retries = new ArrayList<>();
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            retries.add(invocation.getArgument(0));
            return null;
        });
        return retries;
    }

    private String sentSubject() {
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(eq(EMAIL), subject.capture(), anyString());
        return subject.getValue();
    }

    private String sentContent() {
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(eq(EMAIL), anyString(), content.capture());
        return content.getValue();
    }

    @Test
    void 케어_일정_유형이_OFF면_만들지_않는다() {
        NotificationReceiveSetting setting = setting("Y", "Y");
        setting.setCareSchdlNotiRcptnYn("N");
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting);

        service.send(event(1, true));

        verify(sendingLogMapper, never()).insert(any());
        verify(notificationService, never()).create(any());
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 고르지_않은_시점이면_만들지_않는다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("Y", "Y"));

        service.send(event(3, true));

        verify(sendingLogMapper, never()).insert(any());
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 탈퇴_회원이면_만들지_않는다() {
        NotificationReceiveSetting setting = setting("Y", "Y");
        setting.setMbrSttsCd(MemberStatus.WITHDRAWN.getCode());
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting);

        service.send(event(1, true));

        verify(sendingLogMapper, never()).insert(any());
    }

    @Test
    void 두_채널이_모두_OFF면_발송하지_않는다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("N", "N"));

        service.send(event(1, true));

        verify(sendingLogMapper, never()).insert(any());
        verify(notificationService, never()).create(any());
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void 인앱만_ON이면_인앱만_만들고_이력을_남긴다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("N", "Y"));

        service.send(event(1, true));

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).create(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(NotificationType.CARE_SCHEDULE);
        assertThat(captor.getValue().notiTtl()).isEqualTo("[케어 일정] 초코 예방접종 일정이 D-1 남았어요");
        assertThat(captor.getValue().notiCn()).isEqualTo("2026-10-22 14:30 · 냥멍동물병원");
        assertThat(captor.getValue().mvmnPath()).isEqualTo("/health/calendar");
        verify(mailSender, never()).send(anyString(), anyString(), anyString());

        assertThat(claimed).hasSize(1);
        NotificationSendingLog sendingLog = claimed.get(0);
        assertThat(sendingLog.getNotiChnlCd()).isEqualTo(NotificationChannel.INAPP.getCode());
        assertThat(sendingLog.getSchdlSn()).isEqualTo(100L);
        assertThat(sendingLog.getSchdlYmd()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(sendingLog.getBfhdNotiCd()).isEqualTo(NotificationLeadTime.D1.getCode());
        assertThat(sendingLog.getNotiTypeCd()).isEqualTo(NotificationType.CARE_SCHEDULE.getCode());
        verify(sendingLogMapper).updateResult(500L, SendingResult.SUCCESS.getCode(), 0, null, "SYSTEM");
    }

    @Test
    void 이메일만_ON이면_가입_이메일로_치환된_메일을_보낸다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("Y", "N"));

        service.send(event(1, true));

        verify(notificationService, never()).create(any());
        assertThat(sentSubject()).isEqualTo("[냥멍케어] 초코의 예방접종 일정이 D-1 남았어요");
        String content = sentContent();
        assertThat(content).startsWith("진성님, 안녕하세요.\n2026-10-22에 예정된 초코의 예방접종 일정을 안내드립니다.");
        assertThat(content).contains("· 병원 : 냥멍동물병원 · 일시 : 2026-10-22 14:30");
        assertThat(content).contains("케어 캘린더에서 확인하기 : http://localhost:5173/health/calendar");
        assertThat(content).endsWith("설정에서 해제할 수 있습니다 : http://localhost:5173/settings/notifications");
        assertThat(claimed).extracting(NotificationSendingLog::getNotiChnlCd)
                .containsExactly(NotificationChannel.EMAIL.getCode());
        verify(sendingLogMapper).updateResult(500L, SendingResult.SUCCESS.getCode(), 0, null, "SYSTEM");
    }

    @Test
    void 두_채널_ON이면_채널마다_이력을_선점한다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("Y", "Y"));

        service.send(event(1, true));

        assertThat(claimed).extracting(NotificationSendingLog::getNotiChnlCd)
                .containsExactly(NotificationChannel.INAPP.getCode(), NotificationChannel.EMAIL.getCode());
        verify(notificationService).create(any());
        verify(mailSender).send(eq(EMAIL), anyString(), anyString());
    }

    @Test
    void 이미_선점된_알림이면_다시_보내지_않는다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("Y", "Y"));
        doThrow(new DuplicateKeyException("UK_NOTI_SNDNG_LOG_1")).when(sendingLogMapper).insert(any());

        service.send(event(1, true));

        verify(notificationService, never()).create(any());
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        verify(sendingLogMapper, never()).updateResult(any(), anyString(), anyInt(), any(), anyString());
    }

    @Test
    void 당일은_오늘이에요_문형이고_시간_미입력이면_시간을_뺀다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("Y", "Y"));

        service.send(event(0, false));

        assertThat(sentSubject()).isEqualTo("[냥멍케어] 초코의 예방접종 일정이 오늘이에요");
        assertThat(sentContent()).contains("· 병원 : 냥멍동물병원 · 일시 : 2026-10-22\n");
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).create(captor.capture());
        assertThat(captor.getValue().notiTtl()).isEqualTo("[케어 일정] 초코 예방접종 일정이 오늘이에요");
        assertThat(captor.getValue().notiCn()).isEqualTo("2026-10-22 · 냥멍동물병원");
        assertThat(claimed.get(0).getBfhdNotiCd()).isEqualTo(NotificationLeadTime.TODAY.getCode());
    }

    @Test
    void 등록된_템플릿이_있으면_템플릿으로_치환한다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("Y", "N"));
        NotificationTemplate template = new NotificationTemplate();
        template.setNotiTtl("{{반려동물명}} {{D-N}} 알림");
        template.setNotiCn("{{닉네임}}님 {{병원명}} {{일정일}} {{시간}}");
        when(notificationMapper.selectTemplate(NotificationType.CARE_SCHEDULE.getCode(),
                NotificationChannel.EMAIL.getCode())).thenReturn(template);

        service.send(event(1, true));

        assertThat(sentSubject()).isEqualTo("초코 D-1 알림");
        assertThat(sentContent()).startsWith("진성님 냥멍동물병원 2026-10-22 14:30\n\n알림 수신을 원하지 않으시면");
    }

    @Test
    void 메일_실패는_10분_뒤_재시도하고_3회_모두_실패하면_실패로_확정한다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("Y", "N"));
        doThrow(new IllegalStateException("SMTP 오류")).when(mailSender).send(anyString(), anyString(), anyString());

        List<Runnable> retries = collectRetries();

        service.send(event(1, true));

        verify(sendingLogMapper).updateResult(500L, SendingResult.FAIL.getCode(), 0, "SMTP 오류", "SYSTEM");
        for (int rtryNmtm = 1; rtryNmtm <= CareScheduleAlarmService.MAX_RETRY; rtryNmtm++) {
            assertThat(retries).hasSize(rtryNmtm);
            retries.get(rtryNmtm - 1).run();
            verify(sendingLogMapper).updateResult(500L, SendingResult.FAIL.getCode(), rtryNmtm, "SMTP 오류",
                    "SYSTEM");
        }

        verify(mailSender, times(4)).send(anyString(), anyString(), anyString());
        verify(taskScheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
        verify(sendingLogMapper, never()).updateResult(eq(500L), eq(SendingResult.SUCCESS.getCode()), anyInt(),
                isNull(), anyString());
    }

    @Test
    void 재시도에서_성공하면_성공과_재시도_횟수를_남긴다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting("Y", "N"));
        doThrow(new IllegalStateException("SMTP 오류")).doNothing()
                .when(mailSender).send(anyString(), anyString(), anyString());

        List<Runnable> retries = collectRetries();

        service.send(event(1, true));

        assertThat(retries).hasSize(1);
        retries.get(0).run();

        verify(sendingLogMapper).updateResult(500L, SendingResult.SUCCESS.getCode(), 1, null, "SYSTEM");
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }
}
