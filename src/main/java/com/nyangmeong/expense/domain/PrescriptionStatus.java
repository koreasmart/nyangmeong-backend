package com.nyangmeong.expense.domain;

/**
 * [처방 상태 코드]
 * - HLTG06 (PRSC.PRSC_STTS_CD), 영수증 약품 행 기본값 (FN-AIM-009-04)
 */
public enum PrescriptionStatus {
    TAKING("HLTC0010"),
    ENDED("HLTC0011");

    private final String code;

    PrescriptionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
