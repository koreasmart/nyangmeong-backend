package com.nyangmeong.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nyangmeong.pet.service.CareMissionService;

import lombok.RequiredArgsConstructor;

/**
 * [케어미션 정산·출제 배치 (SCR-MYP-020)]
 * - 전날 미션 정산 -> 오늘 미션 출제, 관찰 중 반려동물은 health 몫이라 여기서 넘김
 * - 매시 정각 : 이미 정산·출제한 것은 건너뛰므로 자정에 서버가 꺼져 있던 날도 다음 정각에 채움
 * - app.care-mission.batch-enabled=true 일 때만 등록 : 배포(prod)는 켬, 로컬은 확인할 때만 켬(팀원 PC마다 공용 DB에 쓰지 않게)
 */
@Component
@ConditionalOnProperty(name = "app.care-mission.batch-enabled", havingValue = "true")
@RequiredArgsConstructor
public class CareMissionJob {

    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final CareMissionService careMissionService;
    private final ObservationFake observationFake;

    /**
     * 매시 정각 실행
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void run() {
        run(LocalDate.now(ZONE));
    }

    /**
     * 기준일 정산·출제
     */
    public void run(LocalDate today) {
        careMissionService.settle(today.minusDays(1));
        careMissionService.issue(today, observationFake.search(today));
    }
}
