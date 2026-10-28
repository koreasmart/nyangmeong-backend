package com.nyangmeong.expense.domain;

/**
 * [영수증 OCR 요청 출처 코드]
 * - AIMG07 (RECPT.OCR_DMND_SRC_CD)
 */
public enum ReceiptSource {
    HEALTH("AIMC0026"),
    EXPENSE("AIMC0027");

    private final String code;

    ReceiptSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
