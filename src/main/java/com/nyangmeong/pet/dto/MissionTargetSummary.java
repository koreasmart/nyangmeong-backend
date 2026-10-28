package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [미션 출제 대상]
 * - 주양육 관계 1건 = 반려동물 1마리, 대표 여부는 출제 시점 값
 */
@Data
public class MissionTargetSummary {
    private Long petSn;
    private Long mbrSn;
    private String petSeCd;
    private boolean rprsPetYn;
}
