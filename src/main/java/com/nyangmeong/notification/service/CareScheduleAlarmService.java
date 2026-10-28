package com.nyangmeong.notification.service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.extern.slf4j.Slf4j;

/**
 * [케어 일정 사전 알림 발송 (SCR-AIM-006)]
 * - 수신자 설정 판정 -> 채널별 NOTI_SNDNG_LOG 선점(중복방지키) -> 인앱 생성·메일 발송 -> 결과 기록
 * - 메일 실패 : 10분 간격 최대 3회 재시도, 대기분은 메모리 예약이라 서버 재시작 시 소실
 */
@Slf4j
@Service
public class CareScheduleAlarmService {

    static final int MAX_RETRY = 3;
    static final Duration RETRY_INTERVAL = Duration.ofMinutes(10);
    static final String CALENDAR_PATH = "/health/calendar";
    static final String SETTING_PATH = "/settings/notifications";

    private static final String ACTOR_ID = "SYSTEM";
    private static final int FAIL_RSN_MAX = 1000;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /** 템플릿 미등록 시 기본 문구 (화면설계서 SCR-AIM-006 ①②) */
    static final String DEFAULT_MAIL_TTL = "[냥멍케어] {{반려동물명}}의 {{검진유형}} 일정이 {{D-N}} 남았어요";
    static final String DEFAULT_MAIL_CN = """
            {{닉네임}}님, 안녕하세요.
            {{일정일}}에 예정된 {{반려동물명}}의 {{검진유형}} 일정을 안내드립니다.
            · 병원 : {{병원명}} · 일시 : {{일정일}} {{시간}}

            케어 캘린더에서 확인하기 : {{캘린더링크}}""";
    static final String DEFAULT_INAPP_TTL = "[케어 일정] {{반려동물명}} {{검진유형}} 일정이 {{D-N}} 남았어요";
    static final String DEFAULT_INAPP_CN = "{{일정일}} {{시간}} · {{병원명}}";
    static final String UNSUBSCRIBE_GUIDE = "알림 수신을 원하지 않으시면 설정에서 해제할 수 있습니다 : ";

    private final NotificationMapper notificationMapper;
    private final NotificationSendingLogMapper sendingLogMapper;
    private final NotificationService notificationService;
    private final FieldCryptoService fieldCryptoService;
    private final MailSender mailSender;
    private final TaskScheduler taskScheduler;
    private final String frontBaseUrl;

    public CareScheduleAlarmService(NotificationMapper notificationMapper,
            NotificationSendingLogMapper sendingLogMapper, NotificationService notificationService,
            FieldCryptoService fieldCryptoService, MailSender mailSender, TaskScheduler taskScheduler,
            @Value("${app.front-base-url:http://localhost:5173}") String frontBaseUrl) {
        this.notificationMapper = notificationMapper;
        this.sendingLogMapper = sendingLogMapper;
        this.notificationService = notificationService;
        this.fieldCryptoService = fieldCryptoService;
        this.mailSender = mailSender;
        this.taskScheduler = taskScheduler;
        this.frontBaseUrl = frontBaseUrl;
    }

    /**
     * 사전 알림 발송
     */
    @Transactional
    public void send(CareScheduleAlarmEvent event) {
        NotificationLeadTime leadTime = NotificationLeadTime.fromDays(event.bfhdDayCnt()).orElse(null);
        NotificationReceiveSetting setting = notificationMapper.selectReceiveSetting(event.mbrSn());
        if (leadTime == null || !canReceive(setting, leadTime)) {
            return;
        }
        NotificationRecipient recipient = notificationMapper.selectRecipient(event.mbrSn());
        Map<String, String> values = values(event, recipient);
        if ("Y".equals(setting.getInappNotiRcptnYn())) {
            sendInapp(event, leadTime, values);
        }
        if ("Y".equals(setting.getEmlNotiRcptnYn())) {
            sendMail(event, leadTime, recipient, values);
        }
    }

    /**
     * 수신 여부 판단
     */
    private boolean canReceive(NotificationReceiveSetting setting, NotificationLeadTime leadTime) {
        if (setting == null || MemberStatus.WITHDRAWN.is(setting.getMbrSttsCd())) {
            return false;
        }
        return "Y".equals(setting.getCareSchdlNotiRcptnYn()) && leadTime.isSelected(setting);
    }

    /**
     * 인앱 알림 생성
     */
    private void sendInapp(CareScheduleAlarmEvent event, NotificationLeadTime leadTime, Map<String, String> values) {
        NotificationSendingLog sendingLog = claim(event, leadTime, NotificationChannel.INAPP);
        if (sendingLog == null) {
            return;
        }
        NotificationTemplate template = template(NotificationChannel.INAPP);
        String title = render(titleOf(template, DEFAULT_INAPP_TTL), values, leadTime);
        String content = render(contentOf(template, DEFAULT_INAPP_CN), values, leadTime);
        notificationService.create(
                new NotificationEvent(event.mbrSn(), NotificationType.CARE_SCHEDULE, title, content, CALENDAR_PATH));
        sendingLogMapper.updateResult(sendingLog.getNotiSndngLogSn(), SendingResult.SUCCESS.getCode(), 0, null,
                ACTOR_ID);
    }

    /**
     * 메일 발송
     */
    private void sendMail(CareScheduleAlarmEvent event, NotificationLeadTime leadTime, NotificationRecipient recipient,
            Map<String, String> values) {
        String to = fieldCryptoService.decrypt(recipient.getAcntEmlEnc());
        NotificationSendingLog sendingLog = claim(event, leadTime, NotificationChannel.EMAIL);
        if (sendingLog == null) {
            return;
        }
        NotificationTemplate template = template(NotificationChannel.EMAIL);
        String subject = render(titleOf(template, DEFAULT_MAIL_TTL), values, leadTime);
        /** 수신 해제 안내는 템플릿과 무관하게 하단 고정 (FN-AIM-006-14) */
        String content = render(contentOf(template, DEFAULT_MAIL_CN), values, leadTime)
                + "\n\n" + UNSUBSCRIBE_GUIDE + frontBaseUrl + SETTING_PATH;
        deliver(sendingLog.getNotiSndngLogSn(), to, subject, content, 0);
    }

    /**
     * 메일 발송과 결과 기록
     * - 재시도 회차는 트랜잭션 밖 스케줄러 스레드에서 실행 : 결과 수정 1건씩 자동 커밋
     */
    private void deliver(Long notiSndngLogSn, String to, String subject, String content, int rtryNmtm) {
        try {
            mailSender.send(to, subject, content);
            sendingLogMapper.updateResult(notiSndngLogSn, SendingResult.SUCCESS.getCode(), rtryNmtm, null, ACTOR_ID);
        } catch (RuntimeException e) {
            log.warn("[CareScheduleAlarm] 메일 발송 실패 notiSndngLogSn={} rtryNmtm={}", notiSndngLogSn, rtryNmtm, e);
            sendingLogMapper.updateResult(notiSndngLogSn, SendingResult.FAIL.getCode(), rtryNmtm,
                    failReason(e), ACTOR_ID);
            if (rtryNmtm < MAX_RETRY) {
                taskScheduler.schedule(() -> deliver(notiSndngLogSn, to, subject, content, rtryNmtm + 1),
                        Instant.now().plus(RETRY_INTERVAL));
            }
        }
    }

    /**
     * 채널별 발송 이력 선점
     */
    private NotificationSendingLog claim(CareScheduleAlarmEvent event, NotificationLeadTime leadTime,
            NotificationChannel channel) {
        NotificationSendingLog sendingLog = new NotificationSendingLog();
        sendingLog.setMbrSn(event.mbrSn());
        sendingLog.setSchdlSn(event.schdlSn());
        sendingLog.setSchdlYmd(event.schdlDt().toLocalDate());
        sendingLog.setNotiTypeCd(NotificationType.CARE_SCHEDULE.getCode());
        sendingLog.setNotiChnlCd(channel.getCode());
        sendingLog.setBfhdNotiCd(leadTime.getCode());
        sendingLog.setSndngRsltCd(SendingResult.FAIL.getCode());
        sendingLog.setRtryNmtm(0);
        sendingLog.setRgtrId(ACTOR_ID);
        sendingLog.setMdfrId(ACTOR_ID);
        try {
            sendingLogMapper.insert(sendingLog);
            return sendingLog;
        } catch (DuplicateKeyException e) {
            /** 중복방지키 충돌 = 이미 보낸 알림 (여러 서버 동시 스캔 포함). 그 밖의 DB 오류는 호출자로 */
            log.debug("[CareScheduleAlarm] 이미 보낸 알림 mbrSn={} schdlSn={} channel={}", event.mbrSn(),
                    event.schdlSn(), channel);
            return null;
        }
    }

    /**
     * 채널 템플릿 조회
     */
    private NotificationTemplate template(NotificationChannel channel) {
        return notificationMapper.selectTemplate(NotificationType.CARE_SCHEDULE.getCode(), channel.getCode());
    }

    private String titleOf(NotificationTemplate template, String defaultTitle) {
        return template == null || template.getNotiTtl() == null ? defaultTitle : template.getNotiTtl();
    }

    private String contentOf(NotificationTemplate template, String defaultContent) {
        return template == null ? defaultContent : template.getNotiCn();
    }

    /**
     * 치환 값 목록
     */
    private Map<String, String> values(CareScheduleAlarmEvent event, NotificationRecipient recipient) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("닉네임", recipient == null ? "" : Objects.toString(recipient.getMbrNcnm(), ""));
        values.put("반려동물명", event.petNm());
        values.put("검진유형", Objects.toString(event.ckupTypeNm(), ""));
        values.put("병원명", Objects.toString(event.hsptlNm(), ""));
        values.put("일정일", event.schdlDt().format(DATE_FORMAT));
        values.put("시간", event.hrInptYn() ? event.schdlDt().format(TIME_FORMAT) : "");
        values.put("캘린더링크", frontBaseUrl + CALENDAR_PATH);
        return values;
    }

    /**
     * 템플릿 치환
     * - 당일(D-0)은 "오늘이에요" 문형 (FN-AIM-006-10), 시간 미입력이면 앞 공백까지 생략
     */
    String render(String template, Map<String, String> values, NotificationLeadTime leadTime) {
        boolean isToday = leadTime == NotificationLeadTime.TODAY;
        String result = template
                .replace("{{D-N}} 남았어요", isToday ? "오늘이에요" : "D-" + leadTime.getDays() + " 남았어요")
                .replace("{{D-N}}", isToday ? "오늘" : "D-" + leadTime.getDays());
        if (values.get("시간").isEmpty()) {
            result = result.replace(" {{시간}}", "");
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String failReason(RuntimeException e) {
        String message = Objects.toString(e.getMessage(), e.getClass().getSimpleName());
        return message.length() > FAIL_RSN_MAX ? message.substring(0, FAIL_RSN_MAX) : message;
    }
}
