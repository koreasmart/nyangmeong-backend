package com.nyangmeong.global.notification;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * [케어 일정 사전 알림 이벤트]
 * - EventPublisher.publish() 로만 발행 -> notification 도메인이 커밋 뒤 수신자 설정 판정·발송
 * - bfhdDayCnt : 일정일까지 남은 일수 (0·1·3·7)
 */
public record CareScheduleAlarmEvent(Long mbrSn, Long schdlSn, LocalDateTime schdlDt, boolean hrInptYn,
        String petNm, String ckupTypeNm, String hsptlNm, int bfhdDayCnt) {

    public CareScheduleAlarmEvent {
        Objects.requireNonNull(mbrSn, "mbrSn");
        Objects.requireNonNull(schdlSn, "schdlSn");
        Objects.requireNonNull(schdlDt, "schdlDt");
        Objects.requireNonNull(petNm, "petNm");
    }
}
