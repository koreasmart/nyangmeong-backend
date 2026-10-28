package com.nyangmeong.expense.dto;

import java.time.LocalDate;

/**
 * [가계부 지출 생성 요청]
 * - expense 지출 생성(SJM)에 넘기는 값 : 카테고리 병원, 입력방식 OCR (FN-AIM-009-10)
 * - expndPlcusNm : 사용처(병원명). dtlMttr : 세부사항, 사용자 메모라 영수증 저장은 비운다
 */
public record ExpenseCreateCommand(Long mbrSn, Long petSn, Long recptSn, LocalDate expndYmd, Long expndAmt,
        String expndPlcusNm, String dtlMttr) {
}
