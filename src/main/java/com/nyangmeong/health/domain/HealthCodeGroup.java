package com.nyangmeong.health.domain;

/**
 * [건강기록 공통코드 그룹]
 * - 하위 코드 존재 검사용 그룹 코드
 */
public enum HealthCodeGroup {
    CHECKUP_TYPE("HLTG01"),
    BADGE_COLOR("HLTG21");

    private final String code;

    HealthCodeGroup(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
