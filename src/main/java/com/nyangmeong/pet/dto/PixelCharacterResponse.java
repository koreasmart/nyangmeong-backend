package com.nyangmeong.pet.dto;

import java.util.List;

import com.nyangmeong.pet.domain.CharacterCreationJob;
import com.nyangmeong.pet.domain.PixelCharacter;

/**
 * [픽셀 캐릭터 상세 응답]
 * - 결과 이미지는 GET /api/pets/{petSn}/character-image
 */
public record PixelCharacterResponse(Long pixelCharSn, Long petSn, Long charCrtJobSn, String crtJobSttsCd,
        boolean bscCharSbstYn, boolean inqryRcptYn, List<HeldItemSummary> heldItems) {

    public static PixelCharacterResponse from(PixelCharacter character, CharacterCreationJob job,
            List<HeldItemSummary> heldItems) {
        return new PixelCharacterResponse(character.getPixelCharSn(), character.getPetSn(),
                job != null ? job.getCharCrtJobSn() : null,
                job != null ? job.getCrtJobSttsCd() : null,
                "Y".equals(character.getBscCharSbstYn()),
                job != null && job.getOtoInqrySn() != null,
                heldItems);
    }
}
