package com.nyangmeong.scheduler;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * [관찰 중 반려동물 조회 가짜]
 * - health 관찰 항목 조회(JAJ) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 빈 목록 : 연결 전에는 관찰 기록 미션이 출제되지 않음
 */
@Component
public class ObservationFake {

    /**
     * 관찰 중인 항목이 있는 반려동물 번호 조회
     * - 연결 대상 조건 : OBSRV_ARTCL 관찰 중 상태(HLTG18), 삭제 제외 (FN-MYP-020-03)
     */
    public Set<Long> search(LocalDate baseYmd) {
        return Set.of();
    }
}
