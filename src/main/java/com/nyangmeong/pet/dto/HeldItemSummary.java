package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [보관함 아이템 항목]
 * - 캐릭터 귀속 (FN-MYP-010-14)
 */
@Data
public class HeldItemSummary {
    private Long hldItemSn;
    private Long itemSn;
    private String itemNm;
    private String itemRegnCd;
    private Long atchFileSn;
    private Boolean equipYn;
}
