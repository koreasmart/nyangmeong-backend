package com.nyangmeong.health.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * [일정 목록 검색 조건]
 * - size: 다가오는 일정처럼 앞 N건만 필요할 때 (FN-HLT-003-40)
 */
@Data
public class CareScheduleSearchCond {

    /** 시작일(포함) */
    private LocalDate fromYmd;
    /** 종료일(포함) */
    private LocalDate toYmd;
    /** 일정종류공통코드 */
    private String schdlKndCd;
    /** 검진유형공통코드 */
    private String ckupTypeCd;
    /** 최대 건수 */
    @Min(1)
    @Max(50)
    private Integer size;
}
