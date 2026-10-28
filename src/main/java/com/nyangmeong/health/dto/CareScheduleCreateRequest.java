package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [일정 등록 요청]
 * - 종류별 필수값(병원: 검진유형·병원명, 그 외: 일정명)은 서비스에서 재검증
 */
public record CareScheduleCreateRequest(
        @NotBlank String schdlKndCd,
        @NotNull LocalDateTime schdlDt,
        @NotNull Boolean hrInptYn,
        String ckupTypeCd,
        @Size(max = 80) String hsptlNm,
        @Size(max = 40) String schdlNm,
        @Size(max = 500) String memoCn,
        String cldrBadgeColrCd,
        String schdlSrcCd) {
}
