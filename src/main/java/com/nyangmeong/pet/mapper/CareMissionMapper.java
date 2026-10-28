package com.nyangmeong.pet.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.domain.CareMsn;
import com.nyangmeong.pet.domain.DayMsn;
import com.nyangmeong.pet.dto.DailyMissionSummary;
import com.nyangmeong.pet.dto.MissionTargetSummary;
import com.nyangmeong.pet.dto.TodayMissionSummary;

/**
 * [케어미션 Mapper]
 * - CARE_MSN·DAY_MSN 조회·저장 (FN-MYP-020-03~06)
 */
@Mapper
public interface CareMissionMapper {

    /**
     * 케어미션 마스터 전체 조회
     */
    List<CareMsn> selectMissions();

    /**
     * 미션명으로 사용 중 케어미션 조회
     */
    CareMsn selectMissionByName(@Param("msnNm") String msnNm);

    /**
     * 미션 출제 대상 조회
     */
    List<MissionTargetSummary> selectIssueTargets(@Param("primaryRoleCd") String primaryRoleCd);

    /**
     * 그날 미션이 출제된 반려동물 조회
     */
    List<Long> selectIssuedPetSns(@Param("msnCrtrYmd") LocalDate msnCrtrYmd);

    /**
     * 일별미션 등록
     */
    int insertDayMission(DayMsn dayMsn);

    /**
     * 반려동물의 그날 미션 잠금 조회
     */
    DayMsn selectDayMissionForUpdate(@Param("petSn") Long petSn, @Param("msnCrtrYmd") LocalDate msnCrtrYmd,
            @Param("careMsnSn") Long careMsnSn);

    /**
     * 회원 앞으로 출제된 그날 미션 잠금 조회
     */
    DayMsn selectMemberDayMissionForUpdate(@Param("mbrSn") Long mbrSn, @Param("msnCrtrYmd") LocalDate msnCrtrYmd,
            @Param("careMsnSn") Long careMsnSn);

    /**
     * 일별미션 진행 횟수 수정
     */
    int updateDayMissionProgress(@Param("dayMsnSn") Long dayMsnSn, @Param("prgrsNmtm") int prgrsNmtm);

    /**
     * 일별미션 완료
     */
    int updateDayMissionComplete(@Param("dayMsnSn") Long dayMsnSn, @Param("prgrsNmtm") int prgrsNmtm,
            @Param("cmptnMbrSn") Long cmptnMbrSn, @Param("givePnt") int givePnt);

    /**
     * 정산 전 일별미션 조회
     */
    List<DayMsn> selectUnsettledDayMissions(@Param("msnCrtrYmd") LocalDate msnCrtrYmd);

    /**
     * 일별미션 정산 확정
     */
    int updateDayMissionSettled(@Param("dayMsnSn") Long dayMsnSn, @Param("givePnt") int givePnt);

    /**
     * 반려동물의 그날 미션 목록 조회
     */
    List<DailyMissionSummary> selectDailyMissions(@Param("petSn") Long petSn,
            @Param("msnCrtrYmd") LocalDate msnCrtrYmd);

    /**
     * 회원의 그날 미션 집계
     */
    TodayMissionSummary selectTodaySummary(@Param("mbrSn") Long mbrSn, @Param("msnCrtrYmd") LocalDate msnCrtrYmd);

    /**
     * 반려동물 이름 조회
     */
    String selectPetNm(@Param("petSn") Long petSn);
}
