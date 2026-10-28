package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

/**
 * [검사 - 케어 캘린더 일정]
 */
class CareScheduleServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long OTHER_PET = 20L;
    private static final Long SCHDL = 100L;
    private static final String CHECKUP_TYPE = "HLTC0001";
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 3, 10, 30);

    private final CareScheduleMapper mapper = mock(CareScheduleMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final CareScheduleService service = new CareScheduleService(mapper, petAuthorizer);

    @BeforeEach
    void setUp() {
        when(mapper.existsChildCode(HealthCodeGroup.CHECKUP_TYPE.getCode(), CHECKUP_TYPE)).thenReturn(true);
    }

    private CareScheduleCreateRequest hospital(String ckupTypeCd, String hsptlNm, boolean hrInptYn) {
        return new CareScheduleCreateRequest(ScheduleKind.HOSPITAL.getCode(), AT, hrInptYn, ckupTypeCd, hsptlNm,
                "무시될 일정명", null, null, null);
    }

    private CareScheduleCreateRequest grooming(String schdlNm, String schdlSrcCd, String cldrBadgeColrCd) {
        return new CareScheduleCreateRequest(ScheduleKind.GROOMING.getCode(), AT, true, CHECKUP_TYPE, "병원",
                schdlNm, "메모", cldrBadgeColrCd, schdlSrcCd);
    }

    private Schedule saved(Long petSn, ScheduleKind kind) {
        Schedule schedule = new Schedule();
        schedule.setSchdlSn(SCHDL);
        schedule.setPetSn(petSn);
        schedule.setSchdlKndCd(kind.getCode());
        schedule.setSchdlSrcCd(ScheduleSource.DIRECT.getCode());
        schedule.setSchdlDt(AT);
        schedule.setHrInptYn("Y");
        if (kind == ScheduleKind.HOSPITAL) {
            schedule.setCkupTypeCd(CHECKUP_TYPE);
            schedule.setHsptlNm("튼튼동물병원");
        } else {
            schedule.setSchdlNm("미용");
        }
        return schedule;
    }

    private CareScheduleUpdateRequest update(Long petSn, String schdlKndCd, String schdlNm) {
        return new CareScheduleUpdateRequest(petSn, schdlKndCd, null, null, null, null, schdlNm, null, null);
    }

    private void assertInvalid(Runnable call) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(mapper, never()).insertSchedule(any());
        verify(mapper, never()).updateSchedule(any(), anyString());
    }

    @Test
    void 목록은_권한_검사_뒤_반려동물과_조건으로_조회한다() {
        CareScheduleSearchCond cond = new CareScheduleSearchCond();
        cond.setSize(3);
        List<CareScheduleSummary> rows = List.of(new CareScheduleSummary());
        when(mapper.selectList(PET, cond)).thenReturn(rows);

        assertThat(service.search(MBR, PET, cond)).isSameAs(rows);
        verify(petAuthorizer).checkCaregiver(MBR, PET);
    }

    @Test
    void 권한이_없으면_목록을_조회하지_않는다() {
        doThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED)).when(petAuthorizer).checkCaregiver(MBR, PET);

        assertThatThrownBy(() -> service.search(MBR, PET, new CareScheduleSearchCond()))
                .isInstanceOf(CustomException.class);
        verify(mapper, never()).selectList(anyLong(), any());
    }

    @Test
    void 병원_일정은_일정명을_비우고_직접_출처로_저장한다() {
        CareScheduleResponse res = service.create(MBR, PET, hospital(CHECKUP_TYPE, "튼튼동물병원", true));

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(mapper).insertSchedule(captor.capture());
        Schedule schedule = captor.getValue();
        assertThat(schedule.getPetSn()).isEqualTo(PET);
        assertThat(schedule.getSchdlNm()).isNull();
        assertThat(schedule.getHsptlNm()).isEqualTo("튼튼동물병원");
        assertThat(schedule.getSchdlSrcCd()).isEqualTo(ScheduleSource.DIRECT.getCode());
        assertThat(schedule.getSchdlDt()).isEqualTo(AT);
        assertThat(schedule.getHrInptYn()).isEqualTo("Y");
        assertThat(res.hrInptYn()).isTrue();
        verify(petAuthorizer).checkCaregiver(MBR, PET);
    }

    @Test
    void 시간을_입력하지_않으면_자정으로_저장한다() {
        service.create(MBR, PET, hospital(CHECKUP_TYPE, "튼튼동물병원", false));

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(mapper).insertSchedule(captor.capture());
        assertThat(captor.getValue().getSchdlDt()).isEqualTo(AT.toLocalDate().atStartOfDay());
        assertThat(captor.getValue().getHrInptYn()).isEqualTo("N");
    }

    @Test
    void 병원_일정에_병원명이_없으면_400() {
        assertInvalid(() -> service.create(MBR, PET, hospital(CHECKUP_TYPE, " ", true)));
    }

    @Test
    void 병원_일정에_검진유형이_없거나_없는_코드면_400() {
        assertInvalid(() -> service.create(MBR, PET, hospital(null, "튼튼동물병원", true)));
        assertInvalid(() -> service.create(MBR, PET, hospital("HLTC9999", "튼튼동물병원", true)));
    }

    @Test
    void 병원_외_일정은_일정명이_필수이고_병원_필드를_비운다() {
        assertInvalid(() -> service.create(MBR, PET, grooming(null, null, null)));

        service.create(MBR, PET, grooming("목욕", null, null));
        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(mapper).insertSchedule(captor.capture());
        assertThat(captor.getValue().getSchdlNm()).isEqualTo("목욕");
        assertThat(captor.getValue().getCkupTypeCd()).isNull();
        assertThat(captor.getValue().getHsptlNm()).isNull();
    }

    @Test
    void 없는_일정_종류는_400() {
        assertInvalid(() -> service.create(MBR, PET,
                new CareScheduleCreateRequest("HLTC9999", AT, true, null, null, "일정", null, null, null)));
    }

    @Test
    void AI_대화_등록은_AI_생성_출처로_저장한다() {
        CareScheduleResponse res = service.create(MBR, PET,
                grooming("미용 예약", ScheduleSource.AI_GENERATED.getCode(), null));

        assertThat(res.schdlSrcCd()).isEqualTo(ScheduleSource.AI_GENERATED.getCode());
    }

    @Test
    void 출처_그룹_밖_값은_400() {
        assertInvalid(() -> service.create(MBR, PET, grooming("미용", "HLTC0001", null)));
    }

    @Test
    void 뱃지_색상은_HLTG21_하위_코드만_허용한다() {
        assertInvalid(() -> service.create(MBR, PET, grooming("미용", null, "HLTC9000")));

        when(mapper.existsChildCode(HealthCodeGroup.BADGE_COLOR.getCode(), "HLTC9000")).thenReturn(true);
        assertThat(service.create(MBR, PET, grooming("미용", null, "HLTC9000")).cldrBadgeColrCd())
                .isEqualTo("HLTC9000");
    }

    @Test
    void 반려동물을_바꾸면_새_반려동물_권한을_보고_연결_항목도_옮긴다() {
        when(mapper.selectSchedule(SCHDL)).thenReturn(saved(PET, ScheduleKind.HOSPITAL));

        CareScheduleResponse res = service.update(MBR, PET, SCHDL, update(OTHER_PET, null, null));

        assertThat(res.petSn()).isEqualTo(OTHER_PET);
        verify(petAuthorizer).checkCaregiver(MBR, OTHER_PET);
        verify(mapper).updateSchedule(any(Schedule.class), eq("1"));
        verify(mapper).updateLinkedCareItemsPet(SCHDL, OTHER_PET, "1");
        verify(mapper).updateLinkedRepeatPreventionsPet(SCHDL, OTHER_PET, "1");
    }

    @Test
    void 반려동물이_그대로면_연결_항목을_옮기지_않는다() {
        when(mapper.selectSchedule(SCHDL)).thenReturn(saved(PET, ScheduleKind.HOSPITAL));

        service.update(MBR, PET, SCHDL, update(PET, null, null));

        verify(mapper, never()).updateLinkedCareItemsPet(anyLong(), anyLong(), anyString());
        verify(mapper, never()).updateLinkedRepeatPreventionsPet(anyLong(), anyLong(), anyString());
    }

    @Test
    void 종류를_바꾸면_이전_종류_입력값을_유지하지_않는다() {
        when(mapper.selectSchedule(SCHDL)).thenReturn(saved(PET, ScheduleKind.HOSPITAL));

        assertInvalid(() -> service.update(MBR, PET, SCHDL, update(null, ScheduleKind.ETC.getCode(), null)));

        CareScheduleResponse res = service.update(MBR, PET, SCHDL,
                update(null, ScheduleKind.ETC.getCode(), "기타 일정"));
        assertThat(res.schdlKndCd()).isEqualTo(ScheduleKind.ETC.getCode());
        assertThat(res.hsptlNm()).isNull();
        assertThat(res.ckupTypeCd()).isNull();
        assertThat(res.schdlNm()).isEqualTo("기타 일정");
    }

    @Test
    void 다른_반려동물이나_없는_일정은_404() {
        when(mapper.selectSchedule(SCHDL)).thenReturn(saved(OTHER_PET, ScheduleKind.HOSPITAL));

        assertThatThrownBy(() -> service.update(MBR, PET, SCHDL, update(null, null, null)))
                .extracting("errorCode").isEqualTo(ErrorCode.CARE_SCHEDULE_NOT_FOUND);
        assertThatThrownBy(() -> service.delete(MBR, PET, SCHDL))
                .extracting("errorCode").isEqualTo(ErrorCode.CARE_SCHEDULE_NOT_FOUND);
        assertThatThrownBy(() -> service.delete(MBR, PET, 999L))
                .extracting("errorCode").isEqualTo(ErrorCode.CARE_SCHEDULE_NOT_FOUND);
        verify(mapper, never()).deleteSchedule(anyLong(), anyString());
    }

    @Test
    void 삭제는_일정만_삭제_표시한다() {
        when(mapper.selectSchedule(SCHDL)).thenReturn(saved(PET, ScheduleKind.GROOMING));

        service.delete(MBR, PET, SCHDL);

        verify(petAuthorizer).checkCaregiver(MBR, PET);
        verify(mapper).deleteSchedule(SCHDL, "1");
    }
}
