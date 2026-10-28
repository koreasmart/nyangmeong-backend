package com.nyangmeong.health.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [일정 출처 코드]
 * - HLTG22 (SCHDL.SCHDL_SRC_CD)
 * - AI 대화 등록은 AI_GENERATED (FN-AIM-012-13)
 */
public enum ScheduleSource {
    DIRECT("HLTC0065"),
    CHECKUP("HLTC0066"),
    AI_GENERATED("HLTC0067");

    private final String code;

    ScheduleSource(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 출처 조회
     */
    public static Optional<ScheduleSource> of(String code) {
        return Arrays.stream(values()).filter(source -> source.is(code)).findFirst();
    }
}
