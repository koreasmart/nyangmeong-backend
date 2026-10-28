package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [일정 종류 코드]
 * - HLTG19 (SCHDL.SCHDL_KND_CD)
 */
public enum ScheduleKind {
    HOSPITAL("HLTC0057"),
    GROOMING("HLTC0058"),
    CARE("HLTC0059"),
    ETC("HLTC0060");

    private final String code;

    ScheduleKind(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 종류 조회
     */
    public static Optional<ScheduleKind> of(String code) {
        return Arrays.stream(values()).filter(kind -> kind.is(code)).findFirst();
    }
}
