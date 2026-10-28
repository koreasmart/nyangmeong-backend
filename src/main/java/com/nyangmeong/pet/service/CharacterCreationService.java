package com.nyangmeong.pet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.external.CharacterGenerationRequest;
import com.nyangmeong.global.file.FileExtension;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.pet.domain.CharacterCreationJob;
import com.nyangmeong.pet.domain.CharacterJobStatus;
import com.nyangmeong.pet.domain.CharacterJobStatusHistory;
import com.nyangmeong.pet.domain.CharacterPhoto;
import com.nyangmeong.pet.domain.PhotoRole;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.PetProfileSummary;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

import lombok.RequiredArgsConstructor;

/**
 * [캐릭터 생성 작업 상태 처리]
 * - CharacterCreationWorker 전용 : 시작·완료·실패 각각 별도 트랜잭션
 * - 완료 시 결과 저장 + 오류 확인 중 해제 + 캐릭터 알림 (FN-MYP-010-09·23)
 */
@Service
@RequiredArgsConstructor
public class CharacterCreationService {

    static final String SYSTEM = "SYSTEM";
    static final String MVMN_PATH = "/mypage/characters";
    private static final int FAIL_RSN_MAX = 1000;

    private final PixelCharacterMapper pixelCharacterMapper;
    private final FileStore fileStore;
    private final EventPublisher eventPublisher;

    /**
     * 생성 시작
     */
    @Transactional
    public CharacterGenerationRequest start(Long charCrtJobSn) {
        CharacterCreationJob job = pixelCharacterMapper.selectJob(charCrtJobSn);
        if (job == null || !CharacterJobStatus.REQUESTED.is(job.getCrtJobSttsCd())) {
            return null;
        }
        if (pixelCharacterMapper.updateJobStarted(charCrtJobSn, CharacterJobStatus.REQUESTED.getCode(),
                CharacterJobStatus.CREATING.getCode()) == 0) {
            return null;
        }
        insertHistory(charCrtJobSn, CharacterJobStatus.REQUESTED, CharacterJobStatus.CREATING, null);

        CharacterPhoto original = pixelCharacterMapper.selectJobPhoto(charCrtJobSn, PhotoRole.ORIGINAL.getCode());
        PetProfileSummary pet = pixelCharacterMapper.selectPetProfile(job.getPetSn());
        if (original == null || pet == null) {
            throw new IllegalStateException("생성 입력 없음 charCrtJobSn=" + charCrtJobSn);
        }
        byte[] photo = fileStore.load(original.getAtchFileSn()).content();
        return new CharacterGenerationRequest(photo, pet.getPetSeCd(), pet.getVrtyNm(), pet.getColrNm(), pet.getChrcCn());
    }

    /**
     * 생성 완료
     */
    @Transactional
    public void complete(Long charCrtJobSn, byte[] image) {
        CharacterCreationJob job = pixelCharacterMapper.selectJob(charCrtJobSn);
        if (job == null || !CharacterJobStatus.CREATING.is(job.getCrtJobSttsCd())) {
            return;
        }
        PixelCharacter character = pixelCharacterMapper.selectByPetSn(job.getPetSn());
        PetProfileSummary pet = pixelCharacterMapper.selectPetProfile(job.getPetSn());
        if (character == null || pet == null) {
            throw new IllegalStateException("삭제된 반려동물 charCrtJobSn=" + charCrtJobSn);
        }

        Long atchFileSn = fileStore.storePublic(image, FileExtension.PNG,
                "character-" + character.getPixelCharSn() + ".png", SYSTEM);
        pixelCharacterMapper.updatePhotoHidden(character.getPixelCharSn());
        CharacterPhoto result = new CharacterPhoto();
        result.setPixelCharSn(character.getPixelCharSn());
        result.setCharCrtJobSn(charCrtJobSn);
        result.setAtchFileSn(atchFileSn);
        result.setPhotoRoleCd(PhotoRole.RESULT.getCode());
        result.setSortSeq(0);
        result.setNowIndctYn("Y");
        pixelCharacterMapper.insertPhoto(result);

        pixelCharacterMapper.updateJobFinished(charCrtJobSn, List.of(CharacterJobStatus.CREATING.getCode()),
                CharacterJobStatus.COMPLETED.getCode(), null);
        insertHistory(charCrtJobSn, CharacterJobStatus.CREATING, CharacterJobStatus.COMPLETED, null);
        pixelCharacterMapper.updateBscCharSbstYn(character.getPixelCharSn(), "N", SYSTEM);

        eventPublisher.publish(new NotificationEvent(character.getMbrSn(), NotificationType.CHARACTER,
                pet.getPetNm() + "의 캐릭터가 완성됐어요", null, MVMN_PATH));
    }

    /**
     * 생성 실패
     */
    @Transactional
    public void fail(Long charCrtJobSn, String reason) {
        CharacterCreationJob job = pixelCharacterMapper.selectJob(charCrtJobSn);
        if (job == null || !CharacterJobStatus.isInProgress(job.getCrtJobSttsCd())) {
            return;
        }
        String failRsn = truncate(reason);
        int updated = pixelCharacterMapper.updateJobFinished(charCrtJobSn,
                List.of(CharacterJobStatus.REQUESTED.getCode(), CharacterJobStatus.CREATING.getCode()),
                CharacterJobStatus.FAILED.getCode(), failRsn);
        if (updated > 0) {
            CharacterJobStatus before = CharacterJobStatus.REQUESTED.is(job.getCrtJobSttsCd())
                    ? CharacterJobStatus.REQUESTED
                    : CharacterJobStatus.CREATING;
            insertHistory(charCrtJobSn, before, CharacterJobStatus.FAILED, failRsn);
        }
    }

    /**
     * 상태 이력 등록
     */
    private void insertHistory(Long charCrtJobSn, CharacterJobStatus before, CharacterJobStatus after, String reason) {
        CharacterJobStatusHistory history = new CharacterJobStatusHistory();
        history.setCharCrtJobSn(charCrtJobSn);
        history.setBfrSttsCd(before.getCode());
        history.setAftrSttsCd(after.getCode());
        history.setChgRsn(reason);
        pixelCharacterMapper.insertJobStatusHistory(history);
    }

    /**
     * 실패 사유 길이 제한
     */
    private String truncate(String reason) {
        String value = reason == null || reason.isBlank() ? "알 수 없는 오류" : reason;
        return value.length() > FAIL_RSN_MAX ? value.substring(0, FAIL_RSN_MAX) : value;
    }
}
