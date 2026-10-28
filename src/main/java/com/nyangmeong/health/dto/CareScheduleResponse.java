package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

import com.nyangmeong.health.domain.Schedule;

/**
 * [일정 등록·수정 응답]
 */
public record CareScheduleResponse(
        Long schdlSn,
        Long petSn,
        String schdlKndCd,
        LocalDateTime schdlDt,
        boolean hrInptYn,
        String ckupTypeCd,
        String hsptlNm,
        String schdlNm,
        String memoCn,
        String cldrBadgeColrCd,
        String schdlSrcCd) {

    public static CareScheduleResponse from(Schedule schedule) {
        return new CareScheduleResponse(schedule.getSchdlSn(), schedule.getPetSn(), schedule.getSchdlKndCd(),
                schedule.getSchdlDt(), "Y".equals(schedule.getHrInptYn()), schedule.getCkupTypeCd(),
                schedule.getHsptlNm(), schedule.getSchdlNm(), schedule.getMemoCn(), schedule.getCldrBadgeColrCd(),
                schedule.getSchdlSrcCd());
    }
}
