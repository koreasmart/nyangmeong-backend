package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [그날의 케어미션 행]
 * - GET /pets/{petSn}/daily-missions, 출제 순서 (FN-MYP-020-03·04)
 * - givePnt : 완료도 정산도 안 됐으면 null
 */
@Data
public class DailyMissionSummary {
    private Long dayMsnSn;
    private Long careMsnSn;
    private String msnNm;
    private Integer scorPnt;
    private Integer prgrsNmtm;
    private Integer achvCrtrNmtm;
    private Integer givePnt;
    private boolean cmptnYn;
}
