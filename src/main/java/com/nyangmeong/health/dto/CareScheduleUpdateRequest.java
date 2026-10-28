package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

/**
 * [일정 수정 요청]
 * - 보낸 필드만 수정, 빈 문자열은 값 비움
 * - petSn 변경 시 연결 검진기록·자동 생성 케어 항목도 이동 (FN-HLT-003-37)
 */
public record CareScheduleUpdateRequest(
        Long petSn,
        String schdlKndCd,
        LocalDateTime schdlDt,
        Boolean hrInptYn,
        String ckupTypeCd,
        @Size(max = 80) String hsptlNm,
        @Size(max = 40) String schdlNm,
        @Size(max = 500) String memoCn,
        String cldrBadgeColrCd) {
}
