package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [오늘 미션 집계]
 * - 주양육 반려동물 전체의 오늘 DAY_MSN
 */
@Data
public class TodayMissionSummary {
    private Integer cmptnMsnCnt;
    private Integer msnCnt;
    private Integer petCnt;
}
