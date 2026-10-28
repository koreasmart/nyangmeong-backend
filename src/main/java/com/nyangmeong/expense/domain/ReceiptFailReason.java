package com.nyangmeong.expense.domain;

/**
 * [영수증 일시 오류 사유]
 * - 공통코드 없음 : 이름을 RECPT_STTS_HSTRY.CHG_RSN 에 저장, 화면 문구 2종 분기 (FN-AIM-010-07)
 */
public enum ReceiptFailReason {
    TIMEOUT,
    API_ERROR
}
