package com.nyangmeong.notification.dto;

import java.util.ArrayList;
import java.util.List;

import com.nyangmeong.notification.domain.NotificationLeadTime;
import com.nyangmeong.notification.domain.NotificationReceiveSetting;

/**
 * [알림 설정 응답]
 * - 이메일은 가입 이메일 마스킹 (변경 불가, FN-COM-008-03)
 * - expndLimSetYn: 월 지출 한도 설정 여부 (미설정 시 가계부 이동 안내, FN-COM-008-10)
 */
public record NotificationSettingResponse(String maskedAcntEml, boolean emlNotiRcptnYn, boolean inappNotiRcptnYn,
        boolean careSchdlNotiRcptnYn, List<NotificationLeadTime> leadTimes, boolean mtchDlngNotiRcptnYn,
        boolean cmntNotiRcptnYn, boolean lostExpryNotiRcptnYn, boolean expndLimNotiRcptnYn,
        boolean expndLimSetYn) {

    public static NotificationSettingResponse from(NotificationReceiveSetting setting, String maskedAcntEml) {
        List<NotificationLeadTime> leadTimes = new ArrayList<>();
        if ("Y".equals(setting.getThdyBfhdNotiYn())) {
            leadTimes.add(NotificationLeadTime.TODAY);
        }
        if ("Y".equals(setting.getD1BfhdNotiYn())) {
            leadTimes.add(NotificationLeadTime.D1);
        }
        if ("Y".equals(setting.getD3BfhdNotiYn())) {
            leadTimes.add(NotificationLeadTime.D3);
        }
        if ("Y".equals(setting.getD7BfhdNotiYn())) {
            leadTimes.add(NotificationLeadTime.D7);
        }
        return new NotificationSettingResponse(maskedAcntEml, "Y".equals(setting.getEmlNotiRcptnYn()),
                "Y".equals(setting.getInappNotiRcptnYn()), "Y".equals(setting.getCareSchdlNotiRcptnYn()), leadTimes,
                "Y".equals(setting.getMtchDlngNotiRcptnYn()), "Y".equals(setting.getCmntNotiRcptnYn()),
                "Y".equals(setting.getLostExpryNotiRcptnYn()), "Y".equals(setting.getExpndLimNotiRcptnYn()),
                setting.getMmExpndLimAmt() != null);
    }
}
