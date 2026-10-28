package com.nyangmeong.health.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.Schedule;
import com.nyangmeong.health.domain.ScheduleKind;
import com.nyangmeong.health.domain.ScheduleSource;
import com.nyangmeong.health.dto.CareScheduleCreateRequest;
import com.nyangmeong.health.dto.CareScheduleResponse;
import com.nyangmeong.health.dto.CareScheduleSearchCond;
import com.nyangmeong.health.dto.CareScheduleSummary;
import com.nyangmeong.health.dto.CareScheduleUpdateRequest;
import com.nyangmeong.health.mapper.CareScheduleMapper;

import lombok.RequiredArgsConstructor;

/**
 * [케어 캘린더 일정]
 * - 일정 조회·등록·수정·삭제 (SCR-HLT-003, AI 대화 등록 FN-AIM-012-13·14)
 * - 병원 일정: 검진유형·병원명 필수, 그 외: 일정명 필수 (FN-HLT-003-35)
 */
@Service
@RequiredArgsConstructor
public class CareScheduleService {

    private final CareScheduleMapper careScheduleMapper;
    private final PetAuthorizer petAuthorizer;

    /**
     * 반려동물 일정 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CareScheduleSummary> search(Long mbrSn, Long petSn, CareScheduleSearchCond cond) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        return careScheduleMapper.selectList(petSn, cond);
    }

    /**
     * 일정 등록
     */
    @Transactional
    public CareScheduleResponse create(Long mbrSn, Long petSn, CareScheduleCreateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        ScheduleKind kind = kindOf(req.schdlKndCd());

        Schedule schedule = new Schedule();
        schedule.setPetSn(petSn);
        schedule.setSchdlKndCd(kind.getCode());
        schedule.setSchdlSrcCd(sourceOf(req.schdlSrcCd()).getCode());
        schedule.setHrInptYn(Boolean.TRUE.equals(req.hrInptYn()) ? "Y" : "N");
        schedule.setSchdlDt(req.schdlDt());
        schedule.setCkupTypeCd(emptyToNull(req.ckupTypeCd()));
        schedule.setHsptlNm(emptyToNull(req.hsptlNm()));
        schedule.setSchdlNm(emptyToNull(req.schdlNm()));
        schedule.setMemoCn(emptyToNull(req.memoCn()));
        schedule.setCldrBadgeColrCd(emptyToNull(req.cldrBadgeColrCd()));
        schedule.setRgtrId(String.valueOf(mbrSn));
        schedule.setMdfrId(String.valueOf(mbrSn));
        normalize(schedule, kind);
        check(schedule, kind);

        careScheduleMapper.insertSchedule(schedule);
        return CareScheduleResponse.from(schedule);
    }

    /**
     * 일정 수정
     */
    @Transactional
    public CareScheduleResponse update(Long mbrSn, Long petSn, Long schdlSn, CareScheduleUpdateRequest req) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        Schedule schedule = getOwnSchedule(petSn, schdlSn);

        boolean isPetChanged = req.petSn() != null && !req.petSn().equals(petSn);
        if (isPetChanged) {
            petAuthorizer.checkCaregiver(mbrSn, req.petSn());
            schedule.setPetSn(req.petSn());
        }

        ScheduleKind kind = kindOf(req.schdlKndCd() != null ? req.schdlKndCd() : schedule.getSchdlKndCd());
        /** 종류 전환 시 이전 종류 입력값 미유지 (FN-HLT-003-35) */
        if (!kind.is(schedule.getSchdlKndCd())) {
            schedule.setSchdlKndCd(kind.getCode());
            schedule.setCkupTypeCd(null);
            schedule.setHsptlNm(null);
            schedule.setSchdlNm(null);
        }
        if (req.schdlDt() != null) {
            schedule.setSchdlDt(req.schdlDt());
        }
        if (req.hrInptYn() != null) {
            schedule.setHrInptYn(req.hrInptYn() ? "Y" : "N");
        }
        if (req.ckupTypeCd() != null) {
            schedule.setCkupTypeCd(emptyToNull(req.ckupTypeCd()));
        }
        if (req.hsptlNm() != null) {
            schedule.setHsptlNm(emptyToNull(req.hsptlNm()));
        }
        if (req.schdlNm() != null) {
            schedule.setSchdlNm(emptyToNull(req.schdlNm()));
        }
        if (req.memoCn() != null) {
            schedule.setMemoCn(emptyToNull(req.memoCn()));
        }
        if (req.cldrBadgeColrCd() != null) {
            schedule.setCldrBadgeColrCd(emptyToNull(req.cldrBadgeColrCd()));
        }
        normalize(schedule, kind);
        check(schedule, kind);

        String actorId = String.valueOf(mbrSn);
        careScheduleMapper.updateSchedule(schedule, actorId);
        if (isPetChanged) {
            careScheduleMapper.updateLinkedCareItemsPet(schdlSn, req.petSn(), actorId);
            careScheduleMapper.updateLinkedRepeatPreventionsPet(schdlSn, req.petSn(), actorId);
        }
        return CareScheduleResponse.from(schedule);
    }

    /**
     * 일정 삭제
     */
    @Transactional
    public void delete(Long mbrSn, Long petSn, Long schdlSn) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        getOwnSchedule(petSn, schdlSn);
        careScheduleMapper.deleteSchedule(schdlSn, String.valueOf(mbrSn));
    }

    /**
     * 경로 반려동물의 일정 조회
     */
    private Schedule getOwnSchedule(Long petSn, Long schdlSn) {
        Schedule schedule = careScheduleMapper.selectSchedule(schdlSn);
        if (schedule == null || !petSn.equals(schedule.getPetSn())) {
            throw new CustomException(ErrorCode.CARE_SCHEDULE_NOT_FOUND);
        }
        return schedule;
    }

    /**
     * 종류에 맞지 않는 필드 비움·시간 미입력 시각 정리
     */
    private void normalize(Schedule schedule, ScheduleKind kind) {
        if (kind == ScheduleKind.HOSPITAL) {
            schedule.setSchdlNm(null);
        } else {
            schedule.setCkupTypeCd(null);
            schedule.setHsptlNm(null);
        }
        LocalDateTime schdlDt = schedule.getSchdlDt();
        if (schdlDt != null && !"Y".equals(schedule.getHrInptYn())) {
            schedule.setSchdlDt(schdlDt.toLocalDate().atStartOfDay());
        }
    }

    /**
     * 종류별 필수값·공통코드 검사
     */
    private void check(Schedule schedule, ScheduleKind kind) {
        if (schedule.getSchdlDt() == null) {
            throw invalid();
        }
        if (kind == ScheduleKind.HOSPITAL) {
            if (schedule.getHsptlNm() == null || schedule.getCkupTypeCd() == null
                    || !careScheduleMapper.existsChildCode(HealthCodeGroup.CHECKUP_TYPE.getCode(),
                            schedule.getCkupTypeCd())) {
                throw invalid();
            }
        } else if (schedule.getSchdlNm() == null) {
            throw invalid();
        }
        if (schedule.getCldrBadgeColrCd() != null && !careScheduleMapper
                .existsChildCode(HealthCodeGroup.BADGE_COLOR.getCode(), schedule.getCldrBadgeColrCd())) {
            throw invalid();
        }
    }

    private ScheduleKind kindOf(String schdlKndCd) {
        return ScheduleKind.of(schdlKndCd).orElseThrow(this::invalid);
    }

    private ScheduleSource sourceOf(String schdlSrcCd) {
        if (schdlSrcCd == null || schdlSrcCd.isBlank()) {
            return ScheduleSource.DIRECT;
        }
        return ScheduleSource.of(schdlSrcCd).orElseThrow(this::invalid);
    }

    private CustomException invalid() {
        return new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
