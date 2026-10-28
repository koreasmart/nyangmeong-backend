package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.external.CharacterGenerationRequest;
import com.nyangmeong.global.file.FileExtension;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
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

/**
 * [검사 - 캐릭터 생성 작업 시작·완료·실패]
 */
class CharacterCreationServiceTest {

    private final PixelCharacterMapper mapper = mock(PixelCharacterMapper.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final CharacterCreationService service = new CharacterCreationService(mapper, fileStore, eventPublisher);

    private CharacterCreationJob job(CharacterJobStatus status) {
        CharacterCreationJob job = new CharacterCreationJob();
        job.setCharCrtJobSn(5L);
        job.setPetSn(10L);
        job.setMbrSn(1L);
        job.setCrtJobSttsCd(status.getCode());
        return job;
    }

    private PetProfileSummary pet() {
        PetProfileSummary pet = new PetProfileSummary();
        pet.setPetSn(10L);
        pet.setPetNm("콩이");
        pet.setPetSeCd("PETC0002");
        pet.setVrtyNm("코리안숏헤어");
        pet.setColrNm("고등어");
        pet.setChrcCn("오른쪽 귀 한쪽만 접힘");
        return pet;
    }

    private PixelCharacter character() {
        PixelCharacter character = new PixelCharacter();
        character.setPixelCharSn(3L);
        character.setPetSn(10L);
        character.setMbrSn(1L);
        character.setBscCharSbstYn("N");
        return character;
    }

    @Test
    void 시작하면_생성_중으로_바꾸고_사진과_등록정보를_넘긴다() {
        CharacterPhoto original = new CharacterPhoto();
        original.setAtchFileSn(20L);
        when(mapper.selectJob(5L)).thenReturn(job(CharacterJobStatus.REQUESTED));
        when(mapper.updateJobStarted(5L, CharacterJobStatus.REQUESTED.getCode(), CharacterJobStatus.CREATING.getCode()))
                .thenReturn(1);
        when(mapper.selectJobPhoto(5L, PhotoRole.ORIGINAL.getCode())).thenReturn(original);
        when(mapper.selectPetProfile(10L)).thenReturn(pet());
        when(fileStore.load(20L)).thenReturn(new StoredFile(new byte[] {7}, "image/png", "kongi.png"));

        CharacterGenerationRequest request = service.start(5L);

        assertThat(request.photo()).containsExactly(7);
        assertThat(request.vrtyNm()).isEqualTo("코리안숏헤어");
        assertThat(request.colrNm()).isEqualTo("고등어");
        assertThat(request.chrcCn()).isEqualTo("오른쪽 귀 한쪽만 접힘");
        ArgumentCaptor<CharacterJobStatusHistory> history = ArgumentCaptor.forClass(CharacterJobStatusHistory.class);
        verify(mapper).insertJobStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isEqualTo(CharacterJobStatus.REQUESTED.getCode());
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(CharacterJobStatus.CREATING.getCode());
    }

    @Test
    void 요청_상태가_아니면_시작하지_않는다() {
        when(mapper.selectJob(5L)).thenReturn(job(CharacterJobStatus.CREATING));

        assertThat(service.start(5L)).isNull();
        verify(mapper, never()).updateJobStarted(anyLong(), anyString(), anyString());
    }

    @Test
    void 완료하면_결과를_현재_표시로_저장하고_알림을_발행한다() {
        when(mapper.selectJob(5L)).thenReturn(job(CharacterJobStatus.CREATING));
        when(mapper.selectByPetSn(10L)).thenReturn(character());
        when(mapper.selectPetProfile(10L)).thenReturn(pet());
        when(fileStore.storePublic(any(byte[].class), eq(FileExtension.PNG), anyString(), eq("SYSTEM"))).thenReturn(30L);

        service.complete(5L, new byte[] {1});

        verify(mapper).updatePhotoHidden(3L);
        ArgumentCaptor<CharacterPhoto> photo = ArgumentCaptor.forClass(CharacterPhoto.class);
        verify(mapper).insertPhoto(photo.capture());
        assertThat(photo.getValue().getAtchFileSn()).isEqualTo(30L);
        assertThat(photo.getValue().getPhotoRoleCd()).isEqualTo(PhotoRole.RESULT.getCode());
        assertThat(photo.getValue().getNowIndctYn()).isEqualTo("Y");
        verify(mapper).updateJobFinished(5L, List.of(CharacterJobStatus.CREATING.getCode()),
                CharacterJobStatus.COMPLETED.getCode(), null);
        verify(mapper).updateBscCharSbstYn(3L, "N", "SYSTEM");

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().mbrSn()).isEqualTo(1L);
        assertThat(event.getValue().type()).isEqualTo(NotificationType.CHARACTER);
        assertThat(event.getValue().notiTtl()).isEqualTo("콩이의 캐릭터가 완성됐어요");
        assertThat(event.getValue().mvmnPath()).isEqualTo("/mypage/characters");
    }

    @Test
    void 실패하면_실패_사유를_남기고_알림은_없다() {
        when(mapper.selectJob(5L)).thenReturn(job(CharacterJobStatus.CREATING));
        when(mapper.updateJobFinished(eq(5L), anyList(), eq(CharacterJobStatus.FAILED.getCode()), anyString()))
                .thenReturn(1);

        service.fail(5L, "외부 API 장애");

        verify(mapper).updateJobFinished(5L,
                List.of(CharacterJobStatus.REQUESTED.getCode(), CharacterJobStatus.CREATING.getCode()),
                CharacterJobStatus.FAILED.getCode(), "외부 API 장애");
        ArgumentCaptor<CharacterJobStatusHistory> history = ArgumentCaptor.forClass(CharacterJobStatusHistory.class);
        verify(mapper).insertJobStatusHistory(history.capture());
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(CharacterJobStatus.FAILED.getCode());
        assertThat(history.getValue().getChgRsn()).isEqualTo("외부 API 장애");
        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
    }

    @Test
    void 끝난_작업은_실패로_바꾸지_않는다() {
        when(mapper.selectJob(5L)).thenReturn(job(CharacterJobStatus.COMPLETED));

        service.fail(5L, "늦은 오류");

        verify(mapper, never()).updateJobFinished(anyLong(), anyList(), anyString(), anyString());
    }
}
