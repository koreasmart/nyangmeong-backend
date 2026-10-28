package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.dto.ShopItemSummary;

/**
 * [아이템 Mapper]
 */
@Mapper
public interface ItemMapper {

    /**
     * 아이템 이미지 첨부파일 번호 조회
     */
    Long selectAtchFileSn(Long itemSn);

    /**
     * 상점 아이템 목록 조회 (부위 없으면 전체)
     */
    List<ShopItemSummary> selectShopItems(@Param("pixelCharSn") Long pixelCharSn,
            @Param("itemRegnCd") String itemRegnCd);

    /**
     * 구매할 판매 중 아이템 조회
     */
    List<ShopItemSummary> selectShopItemsBySn(@Param("pixelCharSn") Long pixelCharSn,
            @Param("itemSns") List<Long> itemSns);

    /**
     * 보유 아이템 등록 (미착용)
     */
    int insertHeldItems(@Param("pixelCharSn") Long pixelCharSn, @Param("items") List<ShopItemSummary> items);
}
