package com.nyangmeong.pet.dto;

/**
 * [포인트 요약 응답]
 * - 보유 포인트, 이번 달 적립·사용, 오늘 미션 (FN-MYP-020-01·09, FN-MYP-001-03·04)
 * - 달성률은 프론트가 todayCmptnMsnCnt / todayMsnCnt 로 계산
 */
public record PointResponse(
        Integer hldPnt,
        Integer monthEarnPnt,
        Integer monthEarnCnt,
        Integer monthUsePnt,
        Integer monthUseCnt,
        Integer todayCmptnMsnCnt,
        Integer todayMsnCnt,
        Integer todayMsnPetCnt) {

    public static PointResponse of(int hldPnt, PointMonthSummary month, TodayMissionSummary today) {
        return new PointResponse(hldPnt, month.getEarnPnt(), month.getEarnCnt(), Math.abs(month.getUsePnt()),
                month.getUseCnt(), today.getCmptnMsnCnt(), today.getMsnCnt(), today.getPetCnt());
    }
}
