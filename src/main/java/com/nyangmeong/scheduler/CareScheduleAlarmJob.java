package com.nyangmeong.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.notification.CareScheduleAlarmEvent;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.scheduler.HospitalScheduleFake.HospitalScheduleSummary;

import lombok.RequiredArgsConstructor;

/**
 * [케어 일정 사전 알림 배치 (SCR-AIM-006)]
 * - 매일 09:00 KST 1회 스캔 : 일정일이 D-0·1·3·7 인 병원 일정 × 연동 양육자마다 이벤트 발행
 * - 예약 없음 : 스캔 시점의 일정일·삭제 여부로 판정, 스캔 이후 등록·변경분은 당일 미발송 (FN-AIM-006-18·19)
 */
@Component
@RequiredArgsConstructor
public class CareScheduleAlarmJob {

    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    /** 설정에서 고를 수 있는 사전 알림 일수 (당일·D-1·D-3·D-7) */
    static final List<Integer> BFHD_DAY_CNTS = List.of(0, 1, 3, 7);

    private final HospitalScheduleFake hospitalScheduleFake;
    private final CaregiverFake caregiverFake;
    private final EventPublisher eventPublisher;

    /**
     * 09:00 스캔 실행
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void run() {
        scan(LocalDate.now(ZONE));
    }

    /**
     * 기준일 발송 대상 스캔
     */
    @Transactional
    public void scan(LocalDate baseYmd) {
        for (int bfhdDayCnt : BFHD_DAY_CNTS) {
            for (HospitalScheduleSummary schedule : hospitalScheduleFake.search(baseYmd.plusDays(bfhdDayCnt))) {
                for (Long mbrSn : caregiverFake.search(schedule.petSn())) {
                    eventPublisher.publish(new CareScheduleAlarmEvent(mbrSn, schedule.schdlSn(), schedule.schdlDt(),
                            schedule.hrInptYn(), schedule.petNm(), schedule.ckupTypeNm(), schedule.hsptlNm(),
                            bfhdDayCnt));
                }
            }
        }
    }
}
