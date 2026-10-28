package com.nyangmeong.pet.dto;

import java.util.List;

/**
 * [상점 아이템 목록 응답]
 * - hldPnt : 회원 보유 포인트 (하단 구매 영역 표시)
 */
public record ShopItemListResponse(long hldPnt, List<ShopItemSummary> items) {
}
