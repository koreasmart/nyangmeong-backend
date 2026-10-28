package com.nyangmeong.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.notification.CareScheduleAlarmEvent;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.scheduler.HospitalScheduleFake.HospitalScheduleSummary;

/**
 * [검사 - 케어 일정 사전 알림 스캔]
 */
class CareScheduleAlarmJobTest {

    private static final LocalDate BASE = LocalDate.of(2026, 10, 21);

    private final HospitalScheduleFake hospitalScheduleFake = mock(HospitalScheduleFake.class);
    private final CaregiverFake caregiverFake = mock(CaregiverFake.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final CareScheduleAlarmJob job = new CareScheduleAlarmJob(hospitalScheduleFake, caregiverFake,
            eventPublisher);

    private HospitalScheduleSummary schedule(long schdlSn, LocalDate schdlYmd) {
        return new HospitalScheduleSummary(schdlSn, 10L, "초코", schdlYmd.atTime(14, 30), true, "냥멍동물병원", "예방접종");
    }

    @Test
    void 기준일_D0_1_3_7_일정일만_조회한다() {
        when(hospitalScheduleFake.search(any())).thenReturn(List.of());

        job.scan(BASE);

        verify(hospitalScheduleFake).search(LocalDate.of(2026, 10, 21));
        verify(hospitalScheduleFake).search(LocalDate.of(2026, 10, 22));
        verify(hospitalScheduleFake).search(LocalDate.of(2026, 10, 24));
        verify(hospitalScheduleFake).search(LocalDate.of(2026, 10, 28));
        verify(hospitalScheduleFake, never()).search(LocalDate.of(2026, 10, 23));
        verify(hospitalScheduleFake, times(4)).search(any());
    }

    @Test
    void 일정마다_연동_양육자_수만큼_이벤트를_발행한다() {
        LocalDate d1 = BASE.plusDays(1);
        when(hospitalScheduleFake.search(any())).thenReturn(List.of());
        when(hospitalScheduleFake.search(d1)).thenReturn(List.of(schedule(100L, d1)));
        when(caregiverFake.search(10L)).thenReturn(List.of(1L, 2L));

        job.scan(BASE);

        ArgumentCaptor<CareScheduleAlarmEvent> captor = ArgumentCaptor.forClass(CareScheduleAlarmEvent.class);
        verify(eventPublisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(CareScheduleAlarmEvent::mbrSn).containsExactly(1L, 2L);
        CareScheduleAlarmEvent event = captor.getAllValues().get(0);
        assertThat(event.schdlSn()).isEqualTo(100L);
        assertThat(event.bfhdDayCnt()).isEqualTo(1);
        assertThat(event.schdlDt()).isEqualTo(LocalDateTime.of(2026, 10, 22, 14, 30));
        assertThat(event.petNm()).isEqualTo("초코");
        assertThat(event.hsptlNm()).isEqualTo("냥멍동물병원");
        assertThat(event.ckupTypeNm()).isEqualTo("예방접종");
    }

    @Test
    void 양육자가_없으면_발행하지_않는다() {
        when(hospitalScheduleFake.search(any())).thenReturn(List.of(schedule(100L, BASE)));
        when(caregiverFake.search(10L)).thenReturn(List.of());

        job.scan(BASE);

        verify(eventPublisher, never()).publish(any(CareScheduleAlarmEvent.class));
    }
}
