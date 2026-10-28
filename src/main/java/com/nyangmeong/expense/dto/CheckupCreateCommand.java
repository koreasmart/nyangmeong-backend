package com.nyangmeong.expense.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * [검진기록 생성 요청]
 * - health 검진기록·일정 생성(JAJ)에 넘기는 값
 * - careRfltYn : 처방약은 주기 체크 OFF 로 저장해 투약 항목을 자동 추가하지 않는다 (FN-AIM-009-11)
 * - schdlSn 이 없으면 같은 날 병원 일정에 연결하고 그것도 없으면 일정을 새로 만든다 (FN-AIM-009-09)
 */
public record CheckupCreateCommand(Long mbrSn, Long recptSn, Long petSn, String hsptlNm, LocalDate ckupYmd,
        String ckupTypeCd, String ckupCn, String ckupMemoCn, Long schdlSn, String careRfltYn,
        List<ReceiptSaveDrugRequest> drugs) {
}
