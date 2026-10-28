package com.nyangmeong.pet.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.CharacterSpec;
import com.nyangmeong.global.file.FileExtension;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.pet.domain.CharacterCreationJob;
import com.nyangmeong.pet.domain.CharacterDefaultImage;
import com.nyangmeong.pet.domain.CharacterImagePart;
import com.nyangmeong.pet.domain.CharacterJobStatus;
import com.nyangmeong.pet.domain.CharacterJobStatusHistory;
import com.nyangmeong.pet.domain.CharacterPhoto;
import com.nyangmeong.pet.domain.PhotoRole;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.EquippedItemSaveRequest;
import com.nyangmeong.pet.dto.HeldItemSummary;
import com.nyangmeong.pet.dto.PetProfileSummary;
import com.nyangmeong.pet.dto.PixelCharacterResponse;
import com.nyangmeong.pet.dto.PixelCharacterSummary;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

import lombok.RequiredArgsConstructor;

/**
 * [픽셀 캐릭터 서비스]
 * - SCR-MYP-010 : 목록·생성 요청·꾸미기(착용)·캐릭터 이미지
 * - 캐릭터 경로(/pixel-characters/{pixelCharSn})는 캐릭터의 petSn 으로 권한 검사 (docs/api/issues.md 7번)
 * - 오류 문의 접수·처리 종료는 고객센터·관리자 service 가 호출 (FN-MYP-010-23·24)
 */
@Service
@RequiredArgsConstructor
public class PixelCharacterService {

    private final PixelCharacterMapper pixelCharacterMapper;
    private final PetAuthorizer petAuthorizer;
    private final FileStore fileStore;
    private final EventPublisher eventPublisher;
    private final CharacterCreationWorker characterCreationWorker;

    /**
     * 양육 반려동물별 캐릭터 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PixelCharacterSummary> search(Long mbrSn) {
        return pixelCharacterMapper.selectList(mbrSn);
    }

    /**
     * 캐릭터 상세 조회
     */
    @Transactional(readOnly = true)
    public PixelCharacterResponse getPixelCharacter(Long mbrSn, Long pixelCharSn) {
        PixelCharacter character = getCharacterOrThrow(pixelCharSn);
        petAuthorizer.checkCaregiver(mbrSn, character.getPetSn());
        return toResponse(character);
    }

    /**
     * 캐릭터 생성 요청
     */
    @Transactional
    public PixelCharacterResponse create(Long mbrSn, Long petSn, String idmptDmndId, MultipartFile photo) {
        petAuthorizer.checkPrimary(mbrSn, petSn);
        if (idmptDmndId == null || idmptDmndId.isBlank() || idmptDmndId.length() > 100) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        CharacterCreationJob sameRequest = pixelCharacterMapper.selectJobByIdmptDmndId(idmptDmndId);
        if (sameRequest != null) {
            if (!Objects.equals(sameRequest.getMbrSn(), mbrSn) || !Objects.equals(sameRequest.getPetSn(), petSn)) {
                throw new CustomException(ErrorCode.ALREADY_PROCESSED);
            }
            return toResponse(getCharacterOfPetOrThrow(petSn));
        }
        checkCreatable(petSn);

        String actorId = String.valueOf(mbrSn);
        Long atchFileSn = fileStore.store(photo, actorId);

        PixelCharacter character = new PixelCharacter();
        character.setMbrSn(mbrSn);
        character.setPetSn(petSn);
        character.setMdfrId(actorId);
        CharacterCreationJob job;
        try {
            pixelCharacterMapper.insertPixelCharacter(character);
            job = insertRequestedJob(petSn, mbrSn, idmptDmndId, mbrSn);
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_CREATING);
        }
        insertOriginalPhoto(character.getPixelCharSn(), job.getCharCrtJobSn(), atchFileSn);
        generateAfterCommit(job.getCharCrtJobSn());

        character.setBscCharSbstYn("N");
        return PixelCharacterResponse.from(character, job, List.of());
    }

    /**
     * 착용 상태 저장
     */
    @Transactional
    public PixelCharacterResponse saveEquippedItems(Long mbrSn, Long pixelCharSn, EquippedItemSaveRequest req) {
        PixelCharacter character = getCharacterOrThrow(pixelCharSn);
        petAuthorizer.checkPrimary(mbrSn, character.getPetSn());
        if ("Y".equals(character.getBscCharSbstYn())) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_UNDER_REVIEW);
        }

        Map<Long, HeldItemSummary> held = pixelCharacterMapper.selectHeldItems(pixelCharSn).stream()
                .collect(Collectors.toMap(HeldItemSummary::getHldItemSn, Function.identity()));
        List<Long> hldItemSns = req.hldItemSns().stream().distinct().toList();
        Set<String> regions = new HashSet<>();
        for (Long hldItemSn : hldItemSns) {
            HeldItemSummary item = held.get(hldItemSn);
            if (item == null) {
                throw new CustomException(ErrorCode.ITEM_NOT_HELD);
            }
            if (!regions.add(item.getItemRegnCd())) {
                throw new CustomException(ErrorCode.ITEM_SLOT_DUPLICATED);
            }
        }

        /** 부위별 착용 유일 키(UK_HLD_ITEM_2) 충돌 방지 : 해제 먼저 */
        pixelCharacterMapper.updateUnequipAll(pixelCharSn);
        if (!hldItemSns.isEmpty()) {
            pixelCharacterMapper.updateEquip(pixelCharSn, hldItemSns);
        }
        return toResponse(character);
    }

    /**
     * 캐릭터 이미지 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getCharacterImage(Long mbrSn, Long petSn, CharacterImagePart part) {
        petAuthorizer.checkCaregiver(mbrSn, petSn);
        StoredFile image = currentImage(petSn);
        if (part == CharacterImagePart.FACE) {
            return new StoredFile(CharacterSpec.cropFace(image.content()), FileExtension.PNG.getContentType(),
                    image.orgnlFileNm());
        }
        return image;
    }

    /**
     * 캐릭터 오류 문의 접수
     */
    @Transactional
    public Long receiveErrorInquiry(Long mbrSn, Long pixelCharSn, Long otoInqrySn) {
        PixelCharacter character = getCharacterOrThrow(pixelCharSn);
        petAuthorizer.checkPrimary(mbrSn, character.getPetSn());
        CharacterCreationJob job = pixelCharacterMapper.selectLatestJob(character.getPetSn());
        if (job == null || CharacterJobStatus.isInProgress(job.getCrtJobSttsCd())) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_CREATING);
        }
        CharacterPhoto current = pixelCharacterMapper.selectCurrentPhoto(pixelCharSn);
        if (current == null) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
        }
        if (job.getOtoInqrySn() != null || pixelCharacterMapper.updateJobInquiry(job.getCharCrtJobSn(), otoInqrySn) == 0) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_INQUIRY_ALREADY_RECEIVED);
        }
        pixelCharacterMapper.updateBscCharSbstYn(pixelCharSn, "Y", String.valueOf(mbrSn));
        return current.getAtchFileSn();
    }

    /**
     * 캐릭터 오류 확인 종료
     */
    @Transactional
    public void finishErrorCheck(Long pixelCharSn, boolean regenerate) {
        PixelCharacter character = getCharacterOrThrow(pixelCharSn);
        if (!"Y".equals(character.getBscCharSbstYn())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        CharacterCreationJob latest = pixelCharacterMapper.selectLatestJob(character.getPetSn());
        if (latest != null && CharacterJobStatus.isInProgress(latest.getCrtJobSttsCd())) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_CREATING);
        }
        if (!regenerate) {
            pixelCharacterMapper.updateBscCharSbstYn(pixelCharSn, "N", CharacterCreationService.SYSTEM);
            PetProfileSummary pet = pixelCharacterMapper.selectPetProfile(character.getPetSn());
            String petNm = pet != null ? pet.getPetNm() : "반려동물";
            eventPublisher.publish(new NotificationEvent(character.getMbrSn(), NotificationType.CHARACTER,
                    petNm + "의 캐릭터 오류 확인이 끝났어요", null, CharacterCreationService.MVMN_PATH));
            return;
        }

        /** 오류 확인 중 표시는 재생성 완료까지 유지 (완료 시 CharacterCreationService 가 해제) */
        CharacterPhoto original = latest != null
                ? pixelCharacterMapper.selectJobPhoto(latest.getCharCrtJobSn(), PhotoRole.ORIGINAL.getCode())
                : null;
        if (original == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        CharacterCreationJob job = insertRequestedJob(character.getPetSn(), character.getMbrSn(),
                "REGEN-" + UUID.randomUUID(), null);
        insertOriginalPhoto(pixelCharSn, job.getCharCrtJobSn(), original.getAtchFileSn());
        generateAfterCommit(job.getCharCrtJobSn());
    }

    /**
     * 생성 가능 여부 검사
     */
    private void checkCreatable(Long petSn) {
        if (pixelCharacterMapper.selectByPetSn(petSn) == null) {
            return;
        }
        CharacterCreationJob latest = pixelCharacterMapper.selectLatestJob(petSn);
        if (latest != null && CharacterJobStatus.isInProgress(latest.getCrtJobSttsCd())) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_CREATING);
        }
        throw new CustomException(ErrorCode.PIXEL_CHARACTER_ALREADY_EXISTS);
    }

    /**
     * 요청 상태 생성 작업 등록
     */
    private CharacterCreationJob insertRequestedJob(Long petSn, Long ownerMbrSn, String idmptDmndId, Long actorMbrSn) {
        CharacterCreationJob job = new CharacterCreationJob();
        job.setPetSn(petSn);
        job.setMbrSn(ownerMbrSn);
        job.setCrtJobSttsCd(CharacterJobStatus.REQUESTED.getCode());
        job.setIdmptDmndId(idmptDmndId);
        pixelCharacterMapper.insertJob(job);

        CharacterJobStatusHistory history = new CharacterJobStatusHistory();
        history.setCharCrtJobSn(job.getCharCrtJobSn());
        history.setMbrSn(actorMbrSn);
        history.setAftrSttsCd(CharacterJobStatus.REQUESTED.getCode());
        pixelCharacterMapper.insertJobStatusHistory(history);
        return job;
    }

    /**
     * 원본 사진 등록
     */
    private void insertOriginalPhoto(Long pixelCharSn, Long charCrtJobSn, Long atchFileSn) {
        CharacterPhoto photo = new CharacterPhoto();
        photo.setPixelCharSn(pixelCharSn);
        photo.setCharCrtJobSn(charCrtJobSn);
        photo.setAtchFileSn(atchFileSn);
        photo.setPhotoRoleCd(PhotoRole.ORIGINAL.getCode());
        photo.setSortSeq(0);
        photo.setNowIndctYn("N");
        pixelCharacterMapper.insertPhoto(photo);
    }

    /**
     * 커밋 뒤 백그라운드 생성 예약
     * - 커밋 전 실행 시 작업 행이 보이지 않음
     */
    private void generateAfterCommit(Long charCrtJobSn) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                characterCreationWorker.generate(charCrtJobSn);
            }
        });
    }

    /**
     * 현재 표시 이미지
     * - 결과가 없거나 오류 확인 중이면 종별 기본 캐릭터
     */
    private StoredFile currentImage(Long petSn) {
        PixelCharacter character = pixelCharacterMapper.selectByPetSn(petSn);
        if (character != null && !"Y".equals(character.getBscCharSbstYn())) {
            CharacterPhoto current = pixelCharacterMapper.selectCurrentPhoto(character.getPixelCharSn());
            if (current != null) {
                return fileStore.load(current.getAtchFileSn());
            }
        }
        PetProfileSummary pet = pixelCharacterMapper.selectPetProfile(petSn);
        return defaultImage(CharacterDefaultImage.from(pet != null ? pet.getPetSeCd() : null));
    }

    /**
     * 종별 기본 캐릭터 이미지
     */
    private StoredFile defaultImage(CharacterDefaultImage image) {
        try (InputStream in = new ClassPathResource(image.getPath()).getInputStream()) {
            return new StoredFile(in.readAllBytes(), FileExtension.PNG.getContentType(), image.getPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 캐릭터 조회
     */
    private PixelCharacter getCharacterOrThrow(Long pixelCharSn) {
        PixelCharacter character = pixelCharacterMapper.selectPixelCharacter(pixelCharSn);
        if (character == null) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
        }
        return character;
    }

    /**
     * 반려동물의 캐릭터 조회
     */
    private PixelCharacter getCharacterOfPetOrThrow(Long petSn) {
        PixelCharacter character = pixelCharacterMapper.selectByPetSn(petSn);
        if (character == null) {
            throw new CustomException(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
        }
        return character;
    }

    /**
     * 상세 응답 조립
     */
    private PixelCharacterResponse toResponse(PixelCharacter character) {
        CharacterCreationJob job = pixelCharacterMapper.selectLatestJob(character.getPetSn());
        return PixelCharacterResponse.from(character, job, pixelCharacterMapper.selectHeldItems(character.getPixelCharSn()));
    }
}
