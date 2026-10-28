package com.nyangmeong.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationTab;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.notification.domain.InappNotification;
import com.nyangmeong.notification.domain.NotificationLeadTime;
import com.nyangmeong.notification.domain.NotificationReceiveSetting;
import com.nyangmeong.notification.dto.NotificationSettingResponse;
import com.nyangmeong.notification.dto.NotificationSettingUpdateRequest;
import com.nyangmeong.notification.dto.NotificationSummary;
import com.nyangmeong.notification.dto.UnreadCountResponse;
import com.nyangmeong.notification.mapper.NotificationMapper;

import lombok.RequiredArgsConstructor;

/**
 * [인앱 알림 서비스]
 * - 저장은 NotificationEventListener 로만 (도메인은 EventPublisher 발행)
 * - 수신 OFF 유형은 숨김이 아니라 미생성 (FN-COM-004-13)
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int RECENT_LIMIT = 5;

    private final NotificationMapper notificationMapper;
    private final AuthService authService;

    /**
     * 인앱 알림 생성
     */
    @Transactional
    public void create(NotificationEvent event) {
        NotificationReceiveSetting setting = notificationMapper.selectReceiveSetting(event.mbrSn());
        if (!canReceive(setting, event.type())) {
            return;
        }
        InappNotification notification = new InappNotification();
        notification.setMbrSn(event.mbrSn());
        notification.setNotiTypeCd(event.type().getCode());
        notification.setNotiTtl(event.notiTtl());
        notification.setNotiCn(event.notiCn());
        notification.setMvmnPath(event.mvmnPath());
        notificationMapper.insert(notification);
    }

    /**
     * 미읽음 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long mbrSn) {
        return new UnreadCountResponse(notificationMapper.countUnread(mbrSn));
    }

    /**
     * 탭별 최근 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationSummary> search(Long mbrSn, NotificationTab tab) {
        List<String> notiTypeCds = tab == null || tab == NotificationTab.ALL ? null : NotificationType.codesOf(tab);
        List<NotificationSummary> rows = notificationMapper.selectRecentList(mbrSn, notiTypeCds, RECENT_LIMIT);
        rows.forEach(row -> row.setTab(NotificationType.from(row.getNotiTypeCd()).getTab().name()));
        return rows;
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markRead(Long mbrSn, Long inappNotiSn) {
        if (!notificationMapper.existsOwn(inappNotiSn, mbrSn)) {
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationMapper.updateRead(inappNotiSn, mbrSn);
    }

    /**
     * 알림 설정 조회 (SCR-COM-008, 계정 단위)
     */
    @Transactional(readOnly = true)
    public NotificationSettingResponse getSetting(Long mbrSn) {
        NotificationReceiveSetting setting = notificationMapper.selectReceiveSetting(mbrSn);
        if (setting == null || MemberStatus.WITHDRAWN.is(setting.getMbrSttsCd())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return NotificationSettingResponse.from(setting, authService.getMe(mbrSn).maskedAcntEml());
    }

    /**
     * 알림 설정 저장
     * - 케어 일정 ON 이면 사전 알림 시점 1개 이상
     */
    @Transactional
    public NotificationSettingResponse updateSetting(Long mbrSn, NotificationSettingUpdateRequest req) {
        if (Boolean.TRUE.equals(req.careSchdlNotiRcptnYn()) && req.leadTimes().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        NotificationReceiveSetting setting = new NotificationReceiveSetting();
        setting.setMbrSn(mbrSn);
        setting.setEmlNotiRcptnYn(toYn(req.emlNotiRcptnYn()));
        setting.setInappNotiRcptnYn(toYn(req.inappNotiRcptnYn()));
        setting.setCareSchdlNotiRcptnYn(toYn(req.careSchdlNotiRcptnYn()));
        setting.setThdyBfhdNotiYn(toYn(req.leadTimes().contains(NotificationLeadTime.TODAY)));
        setting.setD1BfhdNotiYn(toYn(req.leadTimes().contains(NotificationLeadTime.D1)));
        setting.setD3BfhdNotiYn(toYn(req.leadTimes().contains(NotificationLeadTime.D3)));
        setting.setD7BfhdNotiYn(toYn(req.leadTimes().contains(NotificationLeadTime.D7)));
        setting.setMtchDlngNotiRcptnYn(toYn(req.mtchDlngNotiRcptnYn()));
        setting.setCmntNotiRcptnYn(toYn(req.cmntNotiRcptnYn()));
        setting.setLostExpryNotiRcptnYn(toYn(req.lostExpryNotiRcptnYn()));
        setting.setExpndLimNotiRcptnYn(toYn(req.expndLimNotiRcptnYn()));
        if (notificationMapper.updateReceiveSetting(setting, String.valueOf(mbrSn)) == 0) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return getSetting(mbrSn);
    }

    private String toYn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }

    /**
     * 수신 여부 판단
     */
    private boolean canReceive(NotificationReceiveSetting setting, NotificationType type) {
        if (setting == null || MemberStatus.WITHDRAWN.is(setting.getMbrSttsCd())) {
            return false;
        }
        if (!"Y".equals(setting.getInappNotiRcptnYn())) {
            return false;
        }
        return switch (type) {
            case CARE_SCHEDULE -> "Y".equals(setting.getCareSchdlNotiRcptnYn());
            case CARE_MATCH -> "Y".equals(setting.getMtchDlngNotiRcptnYn());
            case COMMUNITY -> "Y".equals(setting.getCmntNotiRcptnYn());
            case LOST_EXPIRY -> "Y".equals(setting.getLostExpryNotiRcptnYn());
            case EXPENSE -> "Y".equals(setting.getExpndLimNotiRcptnYn());
            case CHARACTER -> "Y".equals(setting.getCharCrtNotiRcptnYn());
            /** 공동양육·1:1 문의 답변·공지는 설정 없음 */
            case CO_PARENTING, INQUIRY_ANSWER, NOTICE -> true;
        };
    }
}
