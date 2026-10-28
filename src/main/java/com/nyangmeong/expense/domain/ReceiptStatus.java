package com.nyangmeong.expense.domain;

import java.util.Arrays;
import java.util.List;

/**
 * [영수증 OCR 처리 상태 코드]
 * - AIMG01 (RECPT.OCR_PRCS_STTS_CD)
 */
public enum ReceiptStatus {
    ANALYZING("AIMC0001"),
    RECOGNIZED("AIMC0002"),
    PARTIAL("AIMC0003"),
    NOT_RECOGNIZED("AIMC0004"),
    TEMPORARY_ERROR("AIMC0005"),
    CANCELED("AIMC0006"),
    SAVED("AIMC0007");

    /** 다시 분석할 수 있는 상태 (FN-AIM-008-07·010-03) */
    public static final List<ReceiptStatus> RETRYABLE = List.of(CANCELED, NOT_RECOGNIZED, TEMPORARY_ERROR);

    private final String code;

    ReceiptStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 상태 조회
     */
    public static ReceiptStatus from(String code) {
        return Arrays.stream(values())
                .filter(status -> status.is(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 영수증 상태 코드: " + code));
    }
}
