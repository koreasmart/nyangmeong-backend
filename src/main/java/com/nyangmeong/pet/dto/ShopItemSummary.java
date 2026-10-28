package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [상점 아이템 행]
 * - hldYn : 현재 꾸미는 캐릭터 기준 보유 여부 (FN-MYP-011-03)
 */
@Data
public class ShopItemSummary {
    private Long itemSn;
    private String itemNm;
    private String itemRegnCd;
    private Long needPnt;
    private Boolean hldYn;
}
