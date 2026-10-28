package com.nyangmeong.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * [병원 일정 조회 가짜]
 * - health 병원 일정 조회(JAJ) 전까지 쓰는 가짜. 연결 때 지우고 그 service 로 바꾼다
 * - 빈 목록 : NOTI_SNDNG_LOG.SCHDL_SN 외래키로 지어낸 일정 번호 불가, 팀원 서버마다 공용 DB 알림 누적 방지
 */
@Component
public class HospitalScheduleFake {

    /**
     * 일정일의 병원 일정 목록 조회
     * - 연결 대상 조건 : SCHDL_KND_CD 병원(HLTC0057), DEL_YN = 'N' (FN-AIM-006-02)
     */
    public List<HospitalScheduleSummary> search(LocalDate schdlYmd) {
        return List.of();
    }

    /**
     * [병원 일정 요약]
     */
    public record HospitalScheduleSummary(Long schdlSn, Long petSn, String petNm, LocalDateTime schdlDt,
            boolean hrInptYn, String hsptlNm, String ckupTypeNm) {
    }
}
