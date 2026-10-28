package com.nyangmeong.health.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.Schedule;
import com.nyangmeong.health.dto.CareScheduleSearchCond;
import com.nyangmeong.health.dto.CareScheduleSummary;

/**
 * [일정 매퍼]
 * - SCHDL, 반려동물 변경 시 연결 케어 항목·반복 예방 관리 이동
 */
@Mapper
public interface CareScheduleMapper {

    /**
     * 반려동물 일정 목록 조회
     */
    List<CareScheduleSummary> selectList(@Param("petSn") Long petSn, @Param("cond") CareScheduleSearchCond cond);

    /**
     * 일정 단건 조회
     */
    Schedule selectSchedule(Long schdlSn);

    /**
     * 사용 중인 하위 공통코드 존재 여부
     */
    boolean existsChildCode(@Param("prntComCd") String prntComCd, @Param("comCd") String comCd);

    /**
     * 일정 등록
     */
    int insertSchedule(Schedule schedule);

    /**
     * 일정 수정
     */
    int updateSchedule(@Param("schedule") Schedule schedule, @Param("actorId") String actorId);

    /**
     * 일정 삭제
     */
    int deleteSchedule(@Param("schdlSn") Long schdlSn, @Param("actorId") String actorId);

    /**
     * 일정에 연결된 검진기록의 자동 생성 케어 항목 반려동물 이동 (메인 표시 해제)
     */
    int updateLinkedCareItemsPet(@Param("schdlSn") Long schdlSn, @Param("petSn") Long petSn,
            @Param("actorId") String actorId);

    /**
     * 일정에 연결된 검진기록의 반복 예방 관리 반려동물 이동
     */
    int updateLinkedRepeatPreventionsPet(@Param("schdlSn") Long schdlSn, @Param("petSn") Long petSn,
            @Param("actorId") String actorId);
}
