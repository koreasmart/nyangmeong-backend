package com.nyangmeong.seed.core;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * [자동 처리 경고]
 * - 테이블 구조가 바뀌어 생성기가 스스로 메운 것. 같은 문구는 한 번만 모아 실행 끝에 출력한다
 * - 화면에 어색하게 보이면 그 도메인의 Seeder(src/seed/.../domain)에서 값을 다듬는다
 */
public final class SeedWarnings {

    private static final Set<String> MESSAGES = new LinkedHashSet<>();

    private SeedWarnings() {
    }

    public static synchronized void add(String message) {
        MESSAGES.add(message);
    }

    public static synchronized void print() {
        if (MESSAGES.isEmpty()) {
            return;
        }
        System.out.println("[시드] 자동 처리 " + MESSAGES.size() + "건(테이블 구조가 생성기와 달라 스스로 메움):");
        MESSAGES.forEach(m -> System.out.println("  - " + m));
    }
}
