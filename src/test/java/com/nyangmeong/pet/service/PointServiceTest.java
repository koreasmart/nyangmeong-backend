package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.PointDealType;
import com.nyangmeong.pet.domain.PointLedger;
import com.nyangmeong.pet.dto.PointLedgerSummary;
import com.nyangmeong.pet.dto.PointMonthSummary;
import com.nyangmeong.pet.dto.PointResponse;
import com.nyangmeong.pet.dto.TodayMissionSummary;
import com.nyangmeong.pet.mapper.CareMissionMapper;
import com.nyangmeong.pet.mapper.PointMapper;

/**
 * [검사 - 포인트 요약·내역·사용·적립]
 */
class PointServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private final PointMapper pointMapper = mock(PointMapper.class);
    private final CareMissionMapper careMissionMapper = mock(CareMissionMapper.class);
    private final PointService service = new PointService(pointMapper, careMissionMapper);

    @BeforeEach
    void setUp() {
        when(pointMapper.selectHldPnt(MBR)).thenReturn(1_000);
        when(pointMapper.updateHldPnt(anyLong(), anyInt(), anyString())).thenReturn(1);
        when(pointMapper.selectPrimaryMbrSn(PET, BrupRole.PRIMARY.getCode())).thenReturn(MBR);
        when(careMissionMapper.selectPetNm(PET)).thenReturn("뭉치");
    }

    @Test
    void 요약은_이번_달_적립_사용_합계와_건수_오늘_미션을_돌려주고_사용은_절댓값() {
        PointMonthSummary month = new PointMonthSummary();
        month.setEarnPnt(295);
        month.setEarnCnt(4);
        month.setUsePnt(-1_300);
        month.setUseCnt(2);
        TodayMissionSummary today = new TodayMissionSummary();
        today.setCmptnMsnCnt(11);
        today.setMsnCnt(24);
        today.setPetCnt(3);
        when(pointMapper.selectMonthSummary(MBR, LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0))).thenReturn(month);
        when(careMissionMapper.selectTodaySummary(MBR, TODAY)).thenReturn(today);

        PointResponse res = service.getPoint(MBR, TODAY);

        assertThat(res.hldPnt()).isEqualTo(1_000);
        assertThat(res.monthEarnPnt()).isEqualTo(295);
        assertThat(res.monthEarnCnt()).isEqualTo(4);
        assertThat(res.monthUsePnt()).isEqualTo(1_300);
        assertThat(res.monthUseCnt()).isEqualTo(2);
        assertThat(res.todayCmptnMsnCnt()).isEqualTo(11);
        assertThat(res.todayMsnCnt()).isEqualTo(24);
        assertThat(res.todayMsnPetCnt()).isEqualTo(3);
    }

    @Test
    void 내역은_고른_달의_첫날부터_다음_달_첫날_전까지_조회한다() {
        PointLedgerSummary row = new PointLedgerSummary();
        row.setPntGnlgrSn(5L);
        when(pointMapper.selectLedgerList(MBR, LocalDateTime.of(2026, 9, 1, 0, 0),
                LocalDateTime.of(2026, 10, 1, 0, 0))).thenReturn(List.of(row));

        assertThat(service.searchLedgers(MBR, YearMonth.of(2026, 9))).extracting(PointLedgerSummary::getPntGnlgrSn)
                .containsExactly(5L);
    }

    @Test
    void 잔액보다_많이_쓰면_POINT_NOT_ENOUGH이고_원장을_남기지_않는다() {
        when(pointMapper.updateHldPnt(MBR, -2_000, "1")).thenReturn(0);

        assertThatThrownBy(() -> service.use(MBR, 2_000, "req-1", "밀짚모자"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.POINT_NOT_ENOUGH);
        verify(pointMapper, never()).insertLedger(any());
    }

    @Test
    void 사용은_음수로_원장에_남기고_거래_후_잔액을_적는다() {
        when(pointMapper.selectHldPnt(MBR)).thenReturn(400);

        service.use(MBR, 600, "req-1", "밀짚모자");

        verify(pointMapper).updateHldPnt(MBR, -600, "1");
        PointLedger ledger = captureLedger();
        assertThat(ledger.getDlngPnt()).isEqualTo(-600);
        assertThat(ledger.getDlngAftrBlncPnt()).isEqualTo(400);
        assertThat(ledger.getPntDlngTypeCd()).isEqualTo(PointDealType.ITEM_PURCHASE.getCode());
        assertThat(ledger.getIdmptDmndId()).isEqualTo("req-1");
        assertThat(ledger.getMemoCn()).isEqualTo("밀짚모자");
        assertThat(ledger.getHldItemSn()).isNull();
    }

    @Test
    void 같은_idmptDmndId로_두_번_쓰면_한_번만_차감한다() {
        when(pointMapper.existsLedger("req-1")).thenReturn(false, true);

        service.use(MBR, 600, "req-1", "밀짚모자");
        service.use(MBR, 600, "req-1", "밀짚모자");

        verify(pointMapper, times(1)).updateHldPnt(MBR, -600, "1");
        verify(pointMapper, times(1)).insertLedger(any());
    }

    @Test
    void 검진_포인트는_주양육자에게_20P를_검진기록_등록으로_적립한다() {
        assertThat(service.earnCheckupPoint(PET, TODAY.minusDays(3), TODAY)).isTrue();

        verify(pointMapper).updateHldPnt(MBR, 20, "SYSTEM");
        PointLedger ledger = captureLedger();
        assertThat(ledger.getMbrSn()).isEqualTo(MBR);
        assertThat(ledger.getPntDlngTypeCd()).isEqualTo(PointDealType.CHECKUP.getCode());
        assertThat(ledger.getIdmptDmndId()).isEqualTo("CKUP-10-20261024");
        assertThat(ledger.getMemoCn()).isEqualTo("뭉치");
    }

    @Test
    void 같은_반려동물_검진일로_두_번_요청하면_한_번만_적립한다() {
        when(pointMapper.existsLedger("CKUP-10-20261024")).thenReturn(false, true);

        assertThat(service.earnCheckupPoint(PET, TODAY.minusDays(3), TODAY)).isTrue();
        assertThat(service.earnCheckupPoint(PET, TODAY.minusDays(3), TODAY)).isFalse();

        verify(pointMapper, times(1)).updateHldPnt(MBR, 20, "SYSTEM");
        verify(pointMapper, times(1)).insertLedger(any());
    }

    @Test
    void 검진일이_7일_전이면_적립하고_8일_전이나_미래면_적립하지_않는다() {
        assertThat(service.earnCheckupPoint(PET, TODAY.minusDays(7), TODAY)).isTrue();
        assertThat(service.earnCheckupPoint(PET, TODAY.minusDays(8), TODAY)).isFalse();
        assertThat(service.earnCheckupPoint(PET, TODAY.plusDays(1), TODAY)).isFalse();

        verify(pointMapper, times(1)).updateHldPnt(anyLong(), anyInt(), anyString());
    }

    @Test
    void 미션_적립은_MSN_dayMsnSn_ID로_원장에_남기고_0P는_적립하지_않는다() {
        service.earnMission(MBR, 77L, 10, "뭉치 · 양치하기");
        service.earnMission(MBR, 78L, 0, "뭉치 · 빗질하기");

        verify(pointMapper, times(1)).updateHldPnt(eq(MBR), anyInt(), anyString());
        PointLedger ledger = captureLedger();
        assertThat(ledger.getIdmptDmndId()).isEqualTo("MSN-77");
        assertThat(ledger.getDayMsnSn()).isEqualTo(77L);
        assertThat(ledger.getPntDlngTypeCd()).isEqualTo(PointDealType.MISSION.getCode());
    }

    private PointLedger captureLedger() {
        ArgumentCaptor<PointLedger> captor = ArgumentCaptor.forClass(PointLedger.class);
        verify(pointMapper).insertLedger(captor.capture());
        return captor.getValue();
    }
}
