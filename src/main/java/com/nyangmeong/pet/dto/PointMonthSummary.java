package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [한 달 포인트 합계]
 * - PNT_GNLGR 집계, usePnt는 음수 합계 그대로
 */
@Data
public class PointMonthSummary {
    private Integer earnPnt;
    private Integer earnCnt;
    private Integer usePnt;
    private Integer useCnt;
}
