package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.nyangmeong.global.auth.PetAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.CharacterSpec;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.pet.domain.CharacterCreationJob;
import com.nyangmeong.pet.domain.CharacterImagePart;
import com.nyangmeong.pet.domain.CharacterJobStatus;
import com.nyangmeong.pet.domain.CharacterPhoto;
import com.nyangmeong.pet.domain.PhotoRole;
import com.nyangmeong.pet.domain.PixelCharacter;
import com.nyangmeong.pet.dto.EquippedItemSaveRequest;
import com.nyangmeong.pet.dto.HeldItemSummary;
import com.nyangmeong.pet.dto.PetProfileSummary;
import com.nyangmeong.pet.dto.PixelCharacterResponse;
import com.nyangmeong.pet.dto.PixelCharacterSummary;
import com.nyangmeong.pet.mapper.PixelCharacterMapper;

/**
 * [검사 - 픽셀 캐릭터 목록·생성 요청·착용·이미지·오류 문의]
 */
class PixelCharacterServiceTest {

    private static final Long MBR = 1L;
    private static final Long PET = 10L;
    private static final Long CHAR = 3L;

    private final PixelCharacterMapper mapper = mock(PixelCharacterMapper.class);
    private final PetAuthorizer petAuthorizer = mock(PetAuthorizer.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final CharacterCreationWorker worker = mock(CharacterCreationWorker.class);
    private final PixelCharacterService service = new PixelCharacterService(mapper, petAuthorizer, fileStore,
            eventPublisher, worker);

    private final MockMultipartFile photo = new MockMultipartFile("photo", "kongi.jpg", "image/jpeg", new byte[] {1});

    @BeforeEach
    void initSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void commit() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    private PixelCharacter character(String bscCharSbstYn) {
        PixelCharacter character = new PixelCharacter();
        character.setPixelCharSn(CHAR);
        character.setPetSn(PET);
        character.setMbrSn(MBR);
        character.setBscCharSbstYn(bscCharSbstYn);
        return character;
    }

    private CharacterCreationJob job(CharacterJobStatus status) {
        CharacterCreationJob job = new CharacterCreationJob();
        job.setCharCrtJobSn(5L);
        job.setPetSn(PET);
        job.setMbrSn(MBR);
        job.setCrtJobSttsCd(status.getCode());
        return job;
    }

    private HeldItemSummary held(Long hldItemSn, String itemRegnCd) {
        HeldItemSummary item = new HeldItemSummary();
        item.setHldItemSn(hldItemSn);
        item.setItemRegnCd(itemRegnCd);
        item.setEquipYn(false);
        return item;
    }

    private CharacterPhoto photo(Long atchFileSn) {
        CharacterPhoto photo = new CharacterPhoto();
        photo.setAtchFileSn(atchFileSn);
        return photo;
    }

    private PetProfileSummary pet(String petSeCd) {
        PetProfileSummary pet = new PetProfileSummary();
        pet.setPetSn(PET);
        pet.setPetNm("콩이");
        pet.setPetSeCd(petSeCd);
        return pet;
    }

    @Test
    void 목록은_주양육과_공동양육을_함께_돌려준다() {
        PixelCharacterSummary mine = new PixelCharacterSummary();
        mine.setPetSn(10L);
        mine.setPixelCharSn(3L);
        PixelCharacterSummary coParent = new PixelCharacterSummary();
        coParent.setPetSn(11L);
        PixelCharacterSummary noCharacter = new PixelCharacterSummary();
        noCharacter.setPetSn(12L);
        when(mapper.selectList(MBR)).thenReturn(List.of(mine, coParent, noCharacter));

        List<PixelCharacterSummary> rows = service.search(MBR);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(2).getPixelCharSn()).isNull();
    }

    @Test
    void 생성_요청은_사진과_작업을_저장하고_커밋_뒤_생성을_시작한다() {
        when(fileStore.store(photo, "1")).thenReturn(20L);
        doAnswer(inv -> {
            inv.<PixelCharacter>getArgument(0).setPixelCharSn(CHAR);
            return null;
        }).when(mapper).insertPixelCharacter(any());
        doAnswer(inv -> {
            inv.<CharacterCreationJob>getArgument(0).setCharCrtJobSn(5L);
            return null;
        }).when(mapper).insertJob(any());

        PixelCharacterResponse res = service.create(MBR, PET, "key-1", photo);

        verify(petAuthorizer).checkPrimary(MBR, PET);
        assertThat(res.pixelCharSn()).isEqualTo(CHAR);
        assertThat(res.charCrtJobSn()).isEqualTo(5L);
        assertThat(res.crtJobSttsCd()).isEqualTo(CharacterJobStatus.REQUESTED.getCode());
        assertThat(res.bscCharSbstYn()).isFalse();
        assertThat(res.heldItems()).isEmpty();

        ArgumentCaptor<CharacterCreationJob> jobCaptor = ArgumentCaptor.forClass(CharacterCreationJob.class);
        verify(mapper).insertJob(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getIdmptDmndId()).isEqualTo("key-1");
        ArgumentCaptor<CharacterPhoto> photoCaptor = ArgumentCaptor.forClass(CharacterPhoto.class);
        verify(mapper).insertPhoto(photoCaptor.capture());
        assertThat(photoCaptor.getValue().getAtchFileSn()).isEqualTo(20L);
        assertThat(photoCaptor.getValue().getPhotoRoleCd()).isEqualTo(PhotoRole.ORIGINAL.getCode());
        assertThat(photoCaptor.getValue().getNowIndctYn()).isEqualTo("N");

        verify(worker, never()).generate(anyLong());
        commit();
        verify(worker).generate(5L);
    }

    @Test
    void 주양육자가_아니면_생성할_수_없다() {
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(petAuthorizer).checkPrimary(2L, PET);

        assertThatThrownBy(() -> service.create(2L, PET, "key-1", photo))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
        verify(fileStore, never()).store(any(MockMultipartFile.class), anyString());
    }

    @Test
    void 요청_식별값이_없으면_IDEMPOTENCY_KEY_REQUIRED() {
        assertThatThrownBy(() -> service.create(MBR, PET, null, photo))
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        assertThatThrownBy(() -> service.create(MBR, PET, " ", photo))
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    void 이미_캐릭터가_있으면_PIXEL_CHARACTER_ALREADY_EXISTS() {
        when(mapper.selectByPetSn(PET)).thenReturn(character("N"));
        when(mapper.selectLatestJob(PET)).thenReturn(job(CharacterJobStatus.COMPLETED));

        assertThatThrownBy(() -> service.create(MBR, PET, "key-2", photo))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_ALREADY_EXISTS);
        verify(mapper, never()).insertPixelCharacter(any());
    }

    @Test
    void 생성_중이면_PIXEL_CHARACTER_CREATING() {
        when(mapper.selectByPetSn(PET)).thenReturn(character("N"));
        when(mapper.selectLatestJob(PET)).thenReturn(job(CharacterJobStatus.CREATING));

        assertThatThrownBy(() -> service.create(MBR, PET, "key-2", photo))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_CREATING);
    }

    @Test
    void 같은_요청_식별값은_새로_만들지_않고_기존_작업을_돌려준다() {
        when(mapper.selectJobByIdmptDmndId("key-1")).thenReturn(job(CharacterJobStatus.CREATING));
        when(mapper.selectByPetSn(PET)).thenReturn(character("N"));
        when(mapper.selectLatestJob(PET)).thenReturn(job(CharacterJobStatus.CREATING));
        when(mapper.selectHeldItems(CHAR)).thenReturn(List.of());

        PixelCharacterResponse res = service.create(MBR, PET, "key-1", photo);

        assertThat(res.charCrtJobSn()).isEqualTo(5L);
        verify(fileStore, never()).store(any(MockMultipartFile.class), anyString());
        verify(mapper, never()).insertJob(any());
        commit();
        verify(worker, never()).generate(anyLong());
    }

    @Test
    void 남의_요청_식별값을_쓰면_ALREADY_PROCESSED() {
        CharacterCreationJob other = job(CharacterJobStatus.COMPLETED);
        other.setPetSn(99L);
        when(mapper.selectJobByIdmptDmndId("key-1")).thenReturn(other);

        assertThatThrownBy(() -> service.create(MBR, PET, "key-1", photo))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);
    }

    @Test
    void 상세는_양육자_권한을_검사하고_보관함과_문의_접수_여부를_준다() {
        CharacterCreationJob job = job(CharacterJobStatus.COMPLETED);
        job.setOtoInqrySn(77L);
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("Y"));
        when(mapper.selectLatestJob(PET)).thenReturn(job);
        when(mapper.selectHeldItems(CHAR)).thenReturn(List.of(held(100L, "MYPC0001")));

        PixelCharacterResponse res = service.getPixelCharacter(2L, CHAR);

        verify(petAuthorizer).checkCaregiver(2L, PET);
        assertThat(res.bscCharSbstYn()).isTrue();
        assertThat(res.inqryRcptYn()).isTrue();
        assertThat(res.heldItems()).extracting(HeldItemSummary::getHldItemSn).containsExactly(100L);
    }

    @Test
    void 없는_캐릭터는_PIXEL_CHARACTER_NOT_FOUND() {
        assertThatThrownBy(() -> service.getPixelCharacter(MBR, 99L))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_NOT_FOUND);
    }

    @Test
    void 착용은_전부_해제한_뒤_요청한_아이템만_착용한다() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(mapper.selectHeldItems(CHAR)).thenReturn(List.of(held(100L, "MYPC0001"), held(101L, "MYPC0004")));

        service.saveEquippedItems(MBR, CHAR, new EquippedItemSaveRequest(List.of(100L, 101L)));

        verify(petAuthorizer).checkPrimary(MBR, PET);
        InOrder order = inOrder(mapper);
        order.verify(mapper).updateUnequipAll(CHAR);
        order.verify(mapper).updateEquip(CHAR, List.of(100L, 101L));
    }

    @Test
    void 빈_목록이면_전부_해제만_한다() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(mapper.selectHeldItems(CHAR)).thenReturn(List.of(held(100L, "MYPC0001")));

        service.saveEquippedItems(MBR, CHAR, new EquippedItemSaveRequest(List.of()));

        verify(mapper).updateUnequipAll(CHAR);
        verify(mapper, never()).updateEquip(anyLong(), any());
    }

    @Test
    void 보유하지_않은_아이템은_ITEM_NOT_HELD() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(mapper.selectHeldItems(CHAR)).thenReturn(List.of(held(100L, "MYPC0001")));

        assertThatThrownBy(() -> service.saveEquippedItems(MBR, CHAR, new EquippedItemSaveRequest(List.of(999L))))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_NOT_HELD);
        verify(mapper, never()).updateUnequipAll(anyLong());
    }

    @Test
    void 같은_부위_두_개는_ITEM_SLOT_DUPLICATED() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(mapper.selectHeldItems(CHAR)).thenReturn(List.of(held(100L, "MYPC0001"), held(101L, "MYPC0001")));

        assertThatThrownBy(() -> service.saveEquippedItems(MBR, CHAR, new EquippedItemSaveRequest(List.of(100L, 101L))))
                .extracting("errorCode").isEqualTo(ErrorCode.ITEM_SLOT_DUPLICATED);
        verify(mapper, never()).updateUnequipAll(anyLong());
    }

    @Test
    void 오류_확인_중이면_꾸밀_수_없다() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("Y"));

        assertThatThrownBy(() -> service.saveEquippedItems(MBR, CHAR, new EquippedItemSaveRequest(List.of())))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_UNDER_REVIEW);
    }

    @Test
    void 공동양육자는_착용을_저장할_수_없다() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        doThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY)).when(petAuthorizer).checkPrimary(2L, PET);

        assertThatThrownBy(() -> service.saveEquippedItems(2L, CHAR, new EquippedItemSaveRequest(List.of())))
                .extracting("errorCode").isEqualTo(ErrorCode.PET_PRIMARY_ONLY);
    }

    @Test
    void 결과가_있으면_결과_이미지를_준다() {
        when(mapper.selectByPetSn(PET)).thenReturn(character("N"));
        when(mapper.selectCurrentPhoto(CHAR)).thenReturn(photo(30L));
        StoredFile result = new StoredFile(new byte[] {5}, "image/png", "character-3.png");
        when(fileStore.load(30L)).thenReturn(result);

        assertThat(service.getCharacterImage(MBR, PET, CharacterImagePart.FULL)).isSameAs(result);
        verify(petAuthorizer).checkCaregiver(MBR, PET);
    }

    @Test
    void 캐릭터가_없으면_종별_기본_이미지를_준다() {
        when(mapper.selectPetProfile(PET)).thenReturn(pet("PETC0001"));

        StoredFile image = service.getCharacterImage(MBR, PET, CharacterImagePart.FULL);

        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.orgnlFileNm()).isEqualTo("character/default-dog.png");
        assertThat(CharacterSpec.matches(image.content())).isTrue();
        verify(fileStore, never()).load(anyLong());
    }

    @Test
    void 오류_확인_중이면_결과가_있어도_기본_이미지를_준다() {
        when(mapper.selectByPetSn(PET)).thenReturn(character("Y"));
        when(mapper.selectPetProfile(PET)).thenReturn(pet("PETC0002"));

        StoredFile image = service.getCharacterImage(MBR, PET, CharacterImagePart.FULL);

        assertThat(image.orgnlFileNm()).isEqualTo("character/default-cat.png");
        verify(mapper, never()).selectCurrentPhoto(anyLong());
    }

    @Test
    void 얼굴만_요청하면_얼굴_영역을_잘라_준다() {
        when(mapper.selectPetProfile(PET)).thenReturn(pet("PETC0002"));

        StoredFile face = service.getCharacterImage(MBR, PET, CharacterImagePart.FACE);

        assertThat(CharacterSpec.matches(face.content())).isFalse();
        assertThat(face.contentType()).isEqualTo("image/png");
    }

    @Test
    void 오류_문의를_접수하면_기본_캐릭터로_전환하고_현재_결과_파일을_준다() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));
        when(mapper.selectLatestJob(PET)).thenReturn(job(CharacterJobStatus.COMPLETED));
        when(mapper.selectCurrentPhoto(CHAR)).thenReturn(photo(30L));
        when(mapper.updateJobInquiry(5L, 77L)).thenReturn(1);

        Long atchFileSn = service.receiveErrorInquiry(MBR, CHAR, 77L);

        assertThat(atchFileSn).isEqualTo(30L);
        verify(petAuthorizer).checkPrimary(MBR, PET);
        verify(mapper).updateBscCharSbstYn(CHAR, "Y", "1");
    }

    @Test
    void 같은_결과에_두_번_접수하면_PIXEL_CHARACTER_INQUIRY_ALREADY_RECEIVED() {
        CharacterCreationJob job = job(CharacterJobStatus.COMPLETED);
        job.setOtoInqrySn(77L);
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("Y"));
        when(mapper.selectLatestJob(PET)).thenReturn(job);
        when(mapper.selectCurrentPhoto(CHAR)).thenReturn(photo(30L));

        assertThatThrownBy(() -> service.receiveErrorInquiry(MBR, CHAR, 78L))
                .extracting("errorCode").isEqualTo(ErrorCode.PIXEL_CHARACTER_INQUIRY_ALREADY_RECEIVED);
        verify(mapper, never()).updateBscCharSbstYn(anyLong(), anyString(), anyString());
    }

    @Test
    void 오류가_아니면_원래_캐릭터로_복원하고_알림을_발행한다() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("Y"));
        when(mapper.selectLatestJob(PET)).thenReturn(job(CharacterJobStatus.COMPLETED));
        when(mapper.selectPetProfile(PET)).thenReturn(pet("PETC0002"));

        service.finishErrorCheck(CHAR, false);

        verify(mapper).updateBscCharSbstYn(CHAR, "N", "SYSTEM");
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().mbrSn()).isEqualTo(MBR);
        verify(mapper, never()).insertJob(any());
    }

    @Test
    void 무료_재생성은_같은_원본으로_새_작업을_만들고_표시는_유지한다() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("Y"));
        when(mapper.selectLatestJob(PET)).thenReturn(job(CharacterJobStatus.COMPLETED));
        when(mapper.selectJobPhoto(5L, PhotoRole.ORIGINAL.getCode())).thenReturn(photo(20L));
        doAnswer(inv -> {
            inv.<CharacterCreationJob>getArgument(0).setCharCrtJobSn(6L);
            return null;
        }).when(mapper).insertJob(any());

        service.finishErrorCheck(CHAR, true);

        ArgumentCaptor<CharacterPhoto> photoCaptor = ArgumentCaptor.forClass(CharacterPhoto.class);
        verify(mapper).insertPhoto(photoCaptor.capture());
        assertThat(photoCaptor.getValue().getCharCrtJobSn()).isEqualTo(6L);
        assertThat(photoCaptor.getValue().getAtchFileSn()).isEqualTo(20L);
        verify(mapper, never()).updateBscCharSbstYn(anyLong(), anyString(), anyString());
        commit();
        verify(worker).generate(6L);
    }

    @Test
    void 오류_확인_중이_아니면_ALREADY_PROCESSED() {
        when(mapper.selectPixelCharacter(CHAR)).thenReturn(character("N"));

        assertThatThrownBy(() -> service.finishErrorCheck(CHAR, false))
                .extracting("errorCode").isEqualTo(ErrorCode.ALREADY_PROCESSED);
    }
}
