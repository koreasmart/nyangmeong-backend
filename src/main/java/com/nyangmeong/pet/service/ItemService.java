package com.nyangmeong.pet.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.ItemPurchaseRequest;
import com.nyangmeong.pet.dto.PixelCharacterResponse;
import com.nyangmeong.pet.dto.ShopItemListResponse;
import com.nyangmeong.pet.dto.ShopItemSummary;
import com.nyangmeong.pet.mapper.ItemMapper;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

import lombok.RequiredArgsConstructor;

/**
 * [아이템 서비스]
 * - 아이템 이미지 = 상점 공용 자원, 반려동물 권한 검사 없음
 * - SCR-MYP-011 상점 : 캐릭터 단위 보유·구매, 주양육자 전용 (FN-MYP-011-06)
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final FileStore fileStore;
    private final PixelCharacterMapper pixelCharacterMapper;
    private final PixelCharacterService pixelCharacterService;
    private final PetAuthorizer petAuthorizer;
    private final PointFake pointFake;

    /**
     * 아이템 이미지 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getImage(Long itemSn) {
        Long atchFileSn = itemMapper.selectAtchFileSn(itemSn);
        if (atchFileSn == null) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }
        return fileStore.load(atchFileSn);
    }

    /**
     * 상점 아이템 목록 조회
     */
    @Transactional(readOnly = true)
    public ShopItemListResponse searchShopItems(Long mbrSn, Long pixelCharSn, String itemRegnCd) {
        checkDecoratable(mbrSn, pixelCharSn, false);
        return new ShopItemListResponse(pointFake.getHldPnt(mbrSn),
                itemMapper.selectShopItems(pixelCharSn, itemRegnCd));
    }

    /**
     * 담은 아이템 일괄 구매
     * - 하나라도 구매 불가면 전체 실패
     */
    @Transactional
    public PixelCharacterResponse purchase(Long mbrSn, Long pixelCharSn, ItemPurchaseRequest req) {
        checkDecoratable(mbrSn, pixelCharSn, true);

        List<Long> itemSns = req.itemSns().stream().distinct().toList();
        List<ShopItemSummary> items = itemMapper.selectShopItemsBySn(pixelCharSn, itemSns);
        if (items.size() != itemSns.size()) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (items.stream().anyMatch(item -> Boolean.TRUE.equals(item.getHldYn()))) {
            throw new CustomException(ErrorCode.ITEM_ALREADY_HELD);
        }

        long totalPnt = items.stream().mapToLong(ShopItemSummary::getNeedPnt).sum();
        pointFake.use(mbrSn, totalPnt, req.idmptDmndId());
        try {
            itemMapper.insertHeldItems(pixelCharSn, items);
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.ITEM_ALREADY_HELD);
        }
        return pixelCharacterService.getPixelCharacter(mbrSn, pixelCharSn);
    }

    /**
     * 꾸밀 캐릭터 존재·주양육자 검사
     * - 구매는 오류 확인 중(기본 캐릭터 대체) 캐릭터 불가
     */
    private void checkDecoratable(Long mbrSn, Long pixelCharSn, boolean checkUnderReview) {
        PixelCharacter character = pixelCharacterMapper.selectPixelCharacter(pixelCharSn);
        if (character == null) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
        }
        petAuthorizer.checkPrimary(mbrSn, character.getPetSn());
        if (checkUnderReview && "Y".equals(character.getBscCharSbstYn())) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_UNDER_REVIEW);
        }
    }
}
