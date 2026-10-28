package com.nyangmeong.pet.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.pet.domain.PointDealType;
import com.nyangmeong.pet.domain.PointLedger;
import com.nyangmeong.pet.dto.PointLedgerSummary;
import com.nyangmeong.pet.dto.PointResponse;
import com.nyangmeong.pet.mapper.CareMissionMapper;
import com.nyangmeong.pet.mapper.PointMapper;

import lombok.RequiredArgsConstructor;

/**
 * [포인트 서비스]
 * - 보유 포인트·요약·내역 (SCR-MYP-020), 적립·차감은 다른 도메인도 부름
 * - 모든 적립·사용은 IDMPT_DMND_ID 기준 1회 : 미션 MSN-{dayMsnSn}, 검진 CKUP-{petSn}-{yyyyMMdd}
 */
@Service
@RequiredArgsConstructor
public class PointService {

    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    /** 검진기록 등록 포인트 (FN-MYP-020-08) */
    static final int CHECKUP_POINT = 20;

    /** 검진 포인트 지급 대상 검진일 범위(등록일 기준 일수) */
    static final int CHECKUP_POINT_DAYS = 7;

    /** 적립 수정자 ID : 회원이 아니라 미션·검진 규칙이 주는 포인트 */
    private static final String SYSTEM = "SYSTEM";

    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

    private final PointMapper pointMapper;
    private final CareMissionMapper careMissionMapper;

    /**
     * 보유 포인트 조회
     */
    @Transactional(readOnly = true)
    public int getHldPnt(Long mbrSn) {
        Integer hldPnt = pointMapper.selectHldPnt(mbrSn);
        return hldPnt == null ? 0 : hldPnt;
    }

    /**
     * 포인트 요약 조회
     */
    @Transactional(readOnly = true)
    public PointResponse getPoint(Long mbrSn) {
        return getPoint(mbrSn, LocalDate.now(ZONE));
    }

    PointResponse getPoint(Long mbrSn, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        return PointResponse.of(getHldPnt(mbrSn),
                pointMapper.selectMonthSummary(mbrSn, month.atDay(1).atStartOfDay(),
                        month.plusMonths(1).atDay(1).atStartOfDay()),
                careMissionMapper.selectTodaySummary(mbrSn, today));
    }

    /**
     * 월 포인트 내역 조회
     */
    @Transactional(readOnly = true)
    public List<PointLedgerSummary> searchLedgers(Long mbrSn, YearMonth yearMonth) {
        YearMonth month = yearMonth == null ? YearMonth.now(ZONE) : yearMonth;
        LocalDateTime fromDt = month.atDay(1).atStartOfDay();
        return pointMapper.selectLedgerList(mbrSn, fromDt, fromDt.plusMonths(1));
    }

    /**
     * 포인트 사용
     */
    @Transactional
    public void use(Long mbrSn, long amount, String idmptDmndId, String memoCn) {
        record(mbrSn, -Math.toIntExact(amount), PointDealType.ITEM_PURCHASE, null, memoCn, idmptDmndId,
                String.valueOf(mbrSn));
    }

    /**
     * 케어미션 포인트 적립
     */
    @Transactional
    public void earnMission(Long mbrSn, Long dayMsnSn, int amount, String memoCn) {
        if (amount <= 0) {
            return;
        }
        record(mbrSn, amount, PointDealType.MISSION, dayMsnSn, memoCn, "MSN-" + dayMsnSn, SYSTEM);
    }

    /**
     * 검진기록 등록 포인트 적립
     */
    @Transactional
    public boolean earnCheckupPoint(Long petSn, LocalDate ckupYmd) {
        return earnCheckupPoint(petSn, ckupYmd, LocalDate.now(ZONE));
    }

    boolean earnCheckupPoint(Long petSn, LocalDate ckupYmd, LocalDate today) {
        if (ckupYmd == null || ckupYmd.isAfter(today) || ckupYmd.isBefore(today.minusDays(CHECKUP_POINT_DAYS))) {
            return false;
        }
        Long primaryMbrSn = pointMapper.selectPrimaryMbrSn(petSn, BrupRole.PRIMARY.getCode());
        if (primaryMbrSn == null) {
            return false;
        }
        return record(primaryMbrSn, CHECKUP_POINT, PointDealType.CHECKUP, null, careMissionMapper.selectPetNm(petSn),
                "CKUP-" + petSn + "-" + ckupYmd.format(YMD), SYSTEM);
    }

    /**
     * 포인트 증감과 원장 기록
     */
    private boolean record(Long mbrSn, int delta, PointDealType type, Long dayMsnSn, String memoCn,
            String idmptDmndId, String actorId) {
        if (pointMapper.existsLedger(idmptDmndId)) {
            return false;
        }
        if (pointMapper.updateHldPnt(mbrSn, delta, actorId) == 0) {
            throw new CustomException(ErrorCode.POINT_NOT_ENOUGH);
        }
        PointLedger ledger = new PointLedger();
        ledger.setMbrSn(mbrSn);
        ledger.setDayMsnSn(dayMsnSn);
        ledger.setPntDlngTypeCd(type.getCode());
        ledger.setDlngPnt(delta);
        ledger.setDlngAftrBlncPnt(getHldPnt(mbrSn));
        ledger.setMemoCn(memoCn);
        ledger.setIdmptDmndId(idmptDmndId);
        pointMapper.insertLedger(ledger);
        return true;
    }
}
