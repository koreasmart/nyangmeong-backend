package com.nyangmeong.expense.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [영수증 저장 전 확인 결과]
 * - duplicateExpenses : 같은 반려동물·날짜·금액·카테고리 병원 지출
 * - nearbySchedules : 같은 날 병원 일정이 없을 때 검진일 앞뒤 7일의 검진기록 미연결 병원 일정
 */
public record ReceiptSaveCheckResponse(List<DuplicateExpense> duplicateExpenses,
        List<NearbySchedule> nearbySchedules) {

    /**
     * 겹치는 지출
     */
    public record DuplicateExpense(Long petSn, Long expndSn, LocalDate expndYmd, Long expndAmt, String dtlMttr,
            String expndPlcusNm) {
    }

    /**
     * 가까운 예정 일정
     */
    public record NearbySchedule(Long petSn, Long schdlSn, LocalDateTime schdlDt, String hsptlNm) {
    }
}
