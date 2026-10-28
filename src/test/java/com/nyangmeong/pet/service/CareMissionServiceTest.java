package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.domain.CareMissionPool;
import com.nyangmeong.pet.domain.CareMsn;
import com.nyangmeong.pet.domain.DayMsn;
import com.nyangmeong.pet.domain.PetSe;
import com.nyangmeong.pet.dto.MissionTargetSummary;
import com.nyangmeong.pet.mapper.CareMissionMapper;

/**
 * [검사 - 케어미션 출제·반영·정산]
 */
class CareMissionServiceTest {

    private static final Long PRIMARY = 1L;
    private static final Long CO_PARENT = 2L;
    private static final Long PET = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    /** 출제 결과를 여러 번 보는 횟수 : 랜덤 출제라 한 번으로는 조건을 확인할 수 없음 */
    private static final int REPEAT = 200;

    private final CareMissionMapper mapper = mock(CareMissionMapper.class);
    private final PointService pointService = mock(PointService.class);
    private final PetAuthorizer authorizer = mock(PetAuthorizer.class);
    private final CareMissionService service = new CareMissionService(mapper, pointService, authorizer);

    private final List<CareMsn> masters = new ArrayList<>();

    @BeforeEach
    void setUp() {
        long sn = 1;
        for (CareMission mission : CareMission.values()) {
            masters.add(master(sn++, mission.getMsnNm(), mission.getPool(), 10, 1, "Y"));
        }
        masters.add(master(sn, "케어리스트 완료하기", CareMissionPool.DAILY, 20, 1, "N"));
        when(mapper.selectMissions()).thenReturn(masters);
        when(mapper.selectIssuedPetSns(any())).thenReturn(List.of());
        when(mapper.selectPetNm(PET)).thenReturn("뭉치");
    }

    @Test
    void 반려동물마다_일상케어_5개와_활동기반_3개를_낸다() {
        target(PetSe.DOG, false);

        service.issue(TODAY, Set.of());

        List<DayMsn> issued = inserted();
        assertThat(issued).hasSize(8);
        assertThat(issued).allSatisfy(d -> {
            assertThat(d.getPetSn()).isEqualTo(PET);
            assertThat(d.getMbrSn()).isEqualTo(PRIMARY);
            assertThat(d.getMsnCrtrYmd()).isEqualTo(TODAY);
            assertThat(d.getScorPnt()).isEqualTo(10);
        });
        assertThat(issued.stream().filter(d -> poolOf(d) == CareMissionPool.DAILY)).hasSize(5);
        assertThat(issued.stream().filter(d -> poolOf(d) == CareMissionPool.ACTIVITY)).hasSize(3);
        assertThat(issued).extracting(DayMsn::getCareMsnSn).doesNotHaveDuplicates();
    }

    @Test
    void 고양이에게는_산책이_없고_대표가_아니면_펫이야기_관찰이_없으면_관찰_기록이_없다() {
        target(PetSe.CAT, false);

        Set<CareMission> seen = issueRepeatedly(Set.of());

        assertThat(seen).doesNotContain(CareMission.WALK_RECORD, CareMission.PET_STORY_POST,
                CareMission.PET_STORY_COMMENT, CareMission.OBSERVATION_RECORD);
    }

    @Test
    void 강아지_대표_관찰_중이면_산책_펫이야기_관찰_기록도_후보가_된다() {
        target(PetSe.DOG, true);

        Set<CareMission> seen = issueRepeatedly(Set.of(PET));

        assertThat(seen).contains(CareMission.WALK_RECORD, CareMission.PET_STORY_POST,
                CareMission.PET_STORY_COMMENT, CareMission.OBSERVATION_RECORD);
    }

    @Test
    void 사용_중지됐거나_케어미션에_없는_행은_내지_않는다() {
        target(PetSe.DOG, true);

        assertThat(issuedSnsRepeatedly(Set.of(PET))).doesNotContain(masters.get(masters.size() - 1).getCareMsnSn());
    }

    @Test
    void 사용_중이어도_케어미션에_없는_행은_사용자_추가_항목처럼_내지_않는다() {
        CareMsn userAdded = master(900L, "사용자 추가 항목", CareMissionPool.DAILY, 10, 1, "Y");
        masters.add(userAdded);
        target(PetSe.CAT, true);

        assertThat(issuedSnsRepeatedly(Set.of(PET))).doesNotContain(userAdded.getCareMsnSn());
    }

    @Test
    void 활동기반_후보가_2개뿐이면_일상케어에서_6개를_낸다() {
        masters.removeIf(m -> CareMissionPool.ACTIVITY.is(m.getMsnPoolCd())
                && !m.getMsnNm().equals(CareMission.EXPENSE_CREATE.getMsnNm())
                && !m.getMsnNm().equals(CareMission.SCHEDULE_CREATE.getMsnNm()));
        target(PetSe.CAT, false);

        service.issue(TODAY, Set.of());

        List<DayMsn> issued = inserted();
        assertThat(issued).hasSize(8);
        assertThat(issued.stream().filter(d -> poolOf(d) == CareMissionPool.DAILY)).hasSize(6);
    }

    @Test
    void 그날_미션이_이미_있는_반려동물은_건너뛴다() {
        target(PetSe.DOG, false);
        when(mapper.selectIssuedPetSns(TODAY)).thenReturn(List.of(PET));

        service.issue(TODAY, Set.of());

        verify(mapper, never()).insertDayMission(any());
    }

    @Test
    void 마지막_1회로_완료되면_즉시_주양육자에게_배점만큼_적립한다() {
        reflectable(CareMission.TOOTH_BRUSHING, 1, day(77L, 0, 5, "N"));

        service.reflect(PET, CareMission.TOOTH_BRUSHING, PRIMARY, TODAY);

        verify(mapper).updateDayMissionComplete(77L, 1, PRIMARY, 5);
        verify(pointService).earnMission(PRIMARY, 77L, 5, "뭉치 · 양치하기");
    }

    @Test
    void 기준_횟수에_못_미치면_진행_횟수만_올린다() {
        reflectable(CareMission.MEAL_RECORD, 3, day(77L, 1, 15, "N"));

        service.reflect(PET, CareMission.MEAL_RECORD, PRIMARY, TODAY);

        verify(mapper).updateDayMissionProgress(77L, 2);
        verify(mapper, never()).updateDayMissionComplete(anyLong(), anyInt(), any(), anyInt());
        verify(pointService, never()).earnMission(any(), any(), anyInt(), any());
    }

    @Test
    void 공동양육자가_완료하면_완료회원은_공동양육자_적립은_주양육자() {
        reflectable(CareMission.WEIGHT_RECORD, 1, day(77L, 0, 10, "N"));

        service.reflect(PET, CareMission.WEIGHT_RECORD, CO_PARENT, TODAY);

        verify(mapper).updateDayMissionComplete(77L, 1, CO_PARENT, 10);
        verify(pointService).earnMission(eq(PRIMARY), eq(77L), eq(10), any());
    }

    @Test
    void 이미_완료했거나_오늘_미션에_없으면_아무것도_하지_않는다() {
        reflectable(CareMission.TOOTH_BRUSHING, 1, day(77L, 1, 5, "Y"));
        service.reflect(PET, CareMission.TOOTH_BRUSHING, PRIMARY, TODAY);

        reflectable(CareMission.BRUSHING, 1, null);
        service.reflect(PET, CareMission.BRUSHING, PRIMARY, TODAY);

        verify(mapper, never()).updateDayMissionComplete(anyLong(), anyInt(), any(), anyInt());
        verify(mapper, never()).updateDayMissionProgress(anyLong(), anyInt());
        verify(pointService, never()).earnMission(any(), any(), anyInt(), any());
    }

    @Test
    void 펫이야기는_회원_앞으로_출제된_미션에_반영한다() {
        CareMsn master = masterOf(CareMission.PET_STORY_COMMENT, 1);
        when(mapper.selectMissionByName(CareMission.PET_STORY_COMMENT.getMsnNm())).thenReturn(master);
        when(mapper.selectMemberDayMissionForUpdate(PRIMARY, TODAY, master.getCareMsnSn()))
                .thenReturn(day(88L, 0, 5, "N"));

        service.reflectRepresentative(PRIMARY, CareMission.PET_STORY_COMMENT, TODAY);

        verify(mapper).updateDayMissionComplete(88L, 1, PRIMARY, 5);
        verify(pointService).earnMission(PRIMARY, 88L, 5, "뭉치 · 펫이야기 댓글 작성하기");
    }

    @Test
    void 정산은_완료분은_다시_적립하지_않고_미완료는_진행률만큼_50퍼센트_이하는_0() {
        CareMsn threeTimes = master(901L, "세 번 미션", CareMissionPool.DAILY, 15, 3, "Y");
        CareMsn twoTimes = master(902L, "두 번 미션", CareMissionPool.DAILY, 10, 2, "Y");
        masters.add(threeTimes);
        masters.add(twoTimes);
        DayMsn done = day(1L, 1, 5, "Y");
        done.setGivePnt(5);
        done.setCareMsnSn(masters.get(0).getCareMsnSn());
        DayMsn twoOfThree = day(2L, 2, 15, "N");
        twoOfThree.setCareMsnSn(901L);
        DayMsn oneOfTwo = day(3L, 1, 10, "N");
        oneOfTwo.setCareMsnSn(902L);
        LocalDate yesterday = TODAY.minusDays(1);
        when(mapper.selectUnsettledDayMissions(yesterday)).thenReturn(List.of(done, twoOfThree, oneOfTwo), List.of());

        service.settle(yesterday);
        service.settle(yesterday);

        verify(mapper).updateDayMissionSettled(1L, 5);
        verify(mapper).updateDayMissionSettled(2L, 10);
        verify(mapper).updateDayMissionSettled(3L, 0);
        verify(pointService, times(1)).earnMission(PRIMARY, 2L, 10, "뭉치 · 세 번 미션");
        verify(pointService, times(1)).earnMission(any(), any(), anyInt(), any());
    }

    @Test
    void 정산_포인트는_명세_예시와_같다() {
        assertThat(CareMissionService.settledPoint(15, 2, 3)).isEqualTo(10);
        assertThat(CareMissionService.settledPoint(10, 1, 2)).isZero();
        assertThat(CareMissionService.settledPoint(10, 0, 1)).isZero();
        assertThat(CareMissionService.settledPoint(20, 3, 4)).isEqualTo(15);
    }

    @Test
    void 미션_조회는_주양육자만_날짜가_없으면_오늘() {
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(authorizer).checkPrimary(CO_PARENT, PET);

        assertThatThrownBy(() -> service.search(CO_PARENT, PET, TODAY))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);

        service.search(PRIMARY, PET, null);
        verify(mapper).selectDailyMissions(PET, LocalDate.now(CareMissionService.ZONE));
    }

    private Set<CareMission> issueRepeatedly(Set<Long> obsrvPetSns) {
        return issuedSnsRepeatedly(obsrvPetSns).stream().map(this::missionOf).collect(Collectors.toSet());
    }

    private List<Long> issuedSnsRepeatedly(Set<Long> obsrvPetSns) {
        List<Long> careMsnSns = new ArrayList<>();
        for (int i = 0; i < REPEAT; i++) {
            clearInvocations(mapper);
            service.issue(TODAY, obsrvPetSns);
            List<DayMsn> issued = inserted();
            assertThat(issued).hasSize(8);
            issued.forEach(d -> careMsnSns.add(d.getCareMsnSn()));
        }
        return careMsnSns;
    }

    private void target(PetSe petSe, boolean representative) {
        MissionTargetSummary target = new MissionTargetSummary();
        target.setPetSn(PET);
        target.setMbrSn(PRIMARY);
        target.setPetSeCd(petSe.getCode());
        target.setRprsPetYn(representative);
        when(mapper.selectIssueTargets(BrupRole.PRIMARY.getCode())).thenReturn(List.of(target));
    }

    private void reflectable(CareMission mission, int achvCrtrNmtm, DayMsn dayMsn) {
        CareMsn master = masterOf(mission, achvCrtrNmtm);
        when(mapper.selectMissionByName(mission.getMsnNm())).thenReturn(master);
        when(mapper.selectDayMissionForUpdate(PET, TODAY, master.getCareMsnSn())).thenReturn(dayMsn);
    }

    private CareMsn masterOf(CareMission mission, int achvCrtrNmtm) {
        CareMsn master = masters.stream().filter(m -> m.getMsnNm().equals(mission.getMsnNm())).findFirst()
                .orElseThrow();
        master.setAchvCrtrNmtm(achvCrtrNmtm);
        return master;
    }

    private List<DayMsn> inserted() {
        ArgumentCaptor<DayMsn> captor = ArgumentCaptor.forClass(DayMsn.class);
        verify(mapper, atLeast(0)).insertDayMission(captor.capture());
        return captor.getAllValues();
    }

    private CareMissionPool poolOf(DayMsn d) {
        return missionOf(d).getPool();
    }

    private CareMission missionOf(DayMsn d) {
        return missionOf(d.getCareMsnSn());
    }

    private CareMission missionOf(Long careMsnSn) {
        Map<Long, CareMsn> bySn = masters.stream().collect(Collectors.toMap(CareMsn::getCareMsnSn, Function.identity()));
        return CareMission.fromMsnNm(bySn.get(careMsnSn).getMsnNm()).orElseThrow();
    }

    private static CareMsn master(long sn, String msnNm, CareMissionPool pool, int cmpnPnt, int achvCrtrNmtm,
            String useYn) {
        CareMsn master = new CareMsn();
        master.setCareMsnSn(sn);
        master.setMsnNm(msnNm);
        master.setMsnPoolCd(pool.getCode());
        master.setCmpnPnt(cmpnPnt);
        master.setAchvCrtrNmtm(achvCrtrNmtm);
        master.setUseYn(useYn);
        return master;
    }

    private static DayMsn day(Long dayMsnSn, int prgrsNmtm, int scorPnt, String cmptnYn) {
        DayMsn day = new DayMsn();
        day.setDayMsnSn(dayMsnSn);
        day.setPetSn(PET);
        day.setMbrSn(PRIMARY);
        day.setPrgrsNmtm(prgrsNmtm);
        day.setScorPnt(scorPnt);
        day.setCmptnYn(cmptnYn);
        day.setClclnCfmtnYn("N");
        return day;
    }
}
