package com.nyangmeong.pet.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.pet.domain.CareMission;
import com.nyangmeong.pet.domain.CareMissionPool;
import com.nyangmeong.pet.domain.CareMsn;
import com.nyangmeong.pet.domain.DayMsn;
import com.nyangmeong.pet.domain.PetSe;
import com.nyangmeong.pet.dto.DailyMissionSummary;
import com.nyangmeong.pet.dto.MissionTargetSummary;
import com.nyangmeong.pet.mapper.CareMissionMapper;

import lombok.RequiredArgsConstructor;

/**
 * [케어미션 서비스]
 * - 출제·자정 정산은 scheduler 배치, 진행 반영은 활동이 일어난 도메인이 부름 (SCR-MYP-020)
 * - 반려동물마다 하루 8개 = 일상케어 5개(매일 할 수 있는 것) + 활동기반 3개(변별력)
 * - 포인트는 출제 때의 주양육자(DAY_MSN.MBR_SN)에게 적립 (FN-MYP-020-06)
 */
@Service
@RequiredArgsConstructor
public class CareMissionService {

    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    /** 일상케어 출제 수 */
    static final int DAILY_COUNT = 5;

    /** 활동기반 출제 수 */
    static final int ACTIVITY_COUNT = 3;

    private final CareMissionMapper careMissionMapper;
    private final PointService pointService;
    private final PetAuthorizer petAuthorizer;

    /**
     * 그날의 케어미션 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DailyMissionSummary> search(Long mbrSn, Long petSn, LocalDate msnCrtrYmd) {
        petAuthorizer.checkPrimary(mbrSn, petSn);
        return careMissionMapper.selectDailyMissions(petSn, msnCrtrYmd == null ? LocalDate.now(ZONE) : msnCrtrYmd);
    }

    /**
     * 그날 케어미션 출제
     */
    @Transactional
    public void issue(LocalDate msnCrtrYmd, Set<Long> obsrvPetSns) {
        Map<CareMission, CareMsn> masters = usableMasters();
        Set<Long> issuedPetSns = new HashSet<>(careMissionMapper.selectIssuedPetSns(msnCrtrYmd));
        for (MissionTargetSummary target : careMissionMapper.selectIssueTargets(BrupRole.PRIMARY.getCode())) {
            if (!issuedPetSns.add(target.getPetSn())) {
                continue;
            }
            for (CareMsn master : pick(masters, target, obsrvPetSns.contains(target.getPetSn()))) {
                DayMsn dayMsn = new DayMsn();
                dayMsn.setCareMsnSn(master.getCareMsnSn());
                dayMsn.setPetSn(target.getPetSn());
                dayMsn.setMbrSn(target.getMbrSn());
                dayMsn.setMsnCrtrYmd(msnCrtrYmd);
                dayMsn.setScorPnt(master.getCmpnPnt());
                careMissionMapper.insertDayMission(dayMsn);
            }
        }
    }

    /**
     * 반려동물 활동의 케어미션 반영
     */
    @Transactional
    public void reflect(Long petSn, CareMission mission, Long actorMbrSn) {
        reflect(petSn, mission, actorMbrSn, LocalDate.now(ZONE));
    }

    void reflect(Long petSn, CareMission mission, Long actorMbrSn, LocalDate today) {
        CareMsn master = careMissionMapper.selectMissionByName(mission.getMsnNm());
        if (master == null) {
            return;
        }
        progress(careMissionMapper.selectDayMissionForUpdate(petSn, today, master.getCareMsnSn()), master,
                actorMbrSn);
    }

    /**
     * 회원 활동의 대표 반려동물 케어미션 반영
     * - 펫이야기 글·댓글 : 대표 반려동물에게만 출제되므로 회원 기준으로 찾음
     */
    @Transactional
    public void reflectRepresentative(Long mbrSn, CareMission mission) {
        reflectRepresentative(mbrSn, mission, LocalDate.now(ZONE));
    }

    void reflectRepresentative(Long mbrSn, CareMission mission, LocalDate today) {
        CareMsn master = careMissionMapper.selectMissionByName(mission.getMsnNm());
        if (master == null) {
            return;
        }
        progress(careMissionMapper.selectMemberDayMissionForUpdate(mbrSn, today, master.getCareMsnSn()), master,
                mbrSn);
    }

    /**
     * 그날 케어미션 정산
     */
    @Transactional
    public void settle(LocalDate msnCrtrYmd) {
        Map<Long, CareMsn> masters = careMissionMapper.selectMissions().stream()
                .collect(Collectors.toMap(CareMsn::getCareMsnSn, Function.identity()));
        for (DayMsn dayMsn : careMissionMapper.selectUnsettledDayMissions(msnCrtrYmd)) {
            if ("Y".equals(dayMsn.getCmptnYn())) {
                careMissionMapper.updateDayMissionSettled(dayMsn.getDayMsnSn(), dayMsn.getGivePnt());
                continue;
            }
            CareMsn master = masters.get(dayMsn.getCareMsnSn());
            int givePnt = settledPoint(dayMsn.getScorPnt(), dayMsn.getPrgrsNmtm(), master.getAchvCrtrNmtm());
            careMissionMapper.updateDayMissionSettled(dayMsn.getDayMsnSn(), givePnt);
            if (givePnt > 0) {
                pointService.earnMission(dayMsn.getMbrSn(), dayMsn.getDayMsnSn(), givePnt,
                        memo(dayMsn.getPetSn(), master));
            }
        }
    }

    /**
     * 미완료 미션의 정산 포인트
     * - 진행률 50% 이하는 0 (FN-MYP-020-05, 15P 미션 2/3 → 10P)
     */
    static int settledPoint(int scorPnt, int prgrsNmtm, int achvCrtrNmtm) {
        if (prgrsNmtm * 2 <= achvCrtrNmtm) {
            return 0;
        }
        return scorPnt * Math.min(prgrsNmtm, achvCrtrNmtm) / achvCrtrNmtm;
    }

    /**
     * 진행 횟수 반영과 완료 시 즉시 지급
     */
    private void progress(DayMsn dayMsn, CareMsn master, Long actorMbrSn) {
        if (dayMsn == null || "Y".equals(dayMsn.getCmptnYn())) {
            return;
        }
        int prgrsNmtm = dayMsn.getPrgrsNmtm() + 1;
        if (prgrsNmtm < master.getAchvCrtrNmtm()) {
            careMissionMapper.updateDayMissionProgress(dayMsn.getDayMsnSn(), prgrsNmtm);
            return;
        }
        careMissionMapper.updateDayMissionComplete(dayMsn.getDayMsnSn(), prgrsNmtm, actorMbrSn, dayMsn.getScorPnt());
        pointService.earnMission(dayMsn.getMbrSn(), dayMsn.getDayMsnSn(), dayMsn.getScorPnt(),
                memo(dayMsn.getPetSn(), master));
    }

    /**
     * 반려동물 1마리의 그날 미션 고르기
     * - 활동기반 후보가 3개보다 적으면 모자란 수만큼 일상케어에서 더 고름
     */
    private List<CareMsn> pick(Map<CareMission, CareMsn> masters, MissionTargetSummary target, boolean observing) {
        boolean dog = PetSe.DOG.is(target.getPetSeCd());
        List<CareMsn> daily = candidates(masters, CareMissionPool.DAILY, dog, observing, target.isRprsPetYn());
        List<CareMsn> activity = candidates(masters, CareMissionPool.ACTIVITY, dog, observing, target.isRprsPetYn());
        Collections.shuffle(daily);
        Collections.shuffle(activity);
        List<CareMsn> picked = new ArrayList<>(activity.subList(0, Math.min(ACTIVITY_COUNT, activity.size())));
        int dailyCount = DAILY_COUNT + ACTIVITY_COUNT - picked.size();
        picked.addAll(daily.subList(0, Math.min(dailyCount, daily.size())));
        Collections.shuffle(picked);
        return picked;
    }

    /**
     * 풀의 출제 후보
     */
    private List<CareMsn> candidates(Map<CareMission, CareMsn> masters, CareMissionPool pool, boolean dog,
            boolean observing, boolean representative) {
        List<CareMsn> result = new ArrayList<>();
        masters.forEach((mission, master) -> {
            if (mission.getPool() == pool && mission.canIssue(dog, observing, representative)) {
                result.add(master);
            }
        });
        return result;
    }

    /**
     * 사용 중 미션 마스터
     * - CareMission 에 없는 행(시드의 옛 미션 등)은 출제하지 않음
     */
    private Map<CareMission, CareMsn> usableMasters() {
        Map<CareMission, CareMsn> masters = new EnumMap<>(CareMission.class);
        for (CareMsn master : careMissionMapper.selectMissions()) {
            if (!"Y".equals(master.getUseYn())) {
                continue;
            }
            Optional<CareMission> mission = CareMission.fromMsnNm(master.getMsnNm());
            mission.ifPresent(m -> masters.put(m, master));
        }
        return masters;
    }

    /**
     * 원장 메모 : {반려동물명} · {미션명}
     */
    private String memo(Long petSn, CareMsn master) {
        return careMissionMapper.selectPetNm(petSn) + " · " + master.getMsnNm();
    }
}
