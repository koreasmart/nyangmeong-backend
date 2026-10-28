package com.nyangmeong.pet.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.external.CharacterGenerationRequest;
import com.nyangmeong.global.external.CharacterGenerator;
import com.nyangmeong.global.external.CharacterProperties;
import com.nyangmeong.global.external.CharacterSpec;
import com.nyangmeong.global.external.FakeCharacterGenerator;

/**
 * [검사 - 캐릭터 백그라운드 생성 (가짜 생성기 성공·실패)]
 */
class CharacterCreationWorkerTest {

    private final CharacterCreationService characterCreationService = mock(CharacterCreationService.class);

    private CharacterGenerationRequest request() {
        return new CharacterGenerationRequest(new byte[] {1}, "PETC0002", "코리안숏헤어", "고등어", "오른쪽 귀 접힘");
    }

    private CharacterCreationWorker worker(String generator) {
        CharacterProperties properties = new CharacterProperties();
        properties.setGenerator(generator);
        return new CharacterCreationWorker(characterCreationService, new FakeCharacterGenerator(properties));
    }

    @Test
    void 가짜_생성기가_성공하면_결과를_완료_처리한다() {
        when(characterCreationService.start(5L)).thenReturn(request());

        worker("fake").generate(5L);

        verify(characterCreationService).complete(eq(5L), any(byte[].class));
        verify(characterCreationService, never()).fail(anyLong(), anyString());
    }

    @Test
    void 가짜_생성기가_실패하면_실패_처리한다() {
        when(characterCreationService.start(5L)).thenReturn(request());

        worker("fake-fail").generate(5L);

        verify(characterCreationService).fail(eq(5L), anyString());
        verify(characterCreationService, never()).complete(anyLong(), any());
    }

    @Test
    void 규격이_다른_결과는_실패_처리한다() {
        when(characterCreationService.start(5L)).thenReturn(request());
        CharacterGenerator wrongSize = req -> CharacterSpec.toPng(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));

        new CharacterCreationWorker(characterCreationService, wrongSize).generate(5L);

        verify(characterCreationService).fail(5L, CharacterCreationWorker.SPEC_MISMATCH);
        verify(characterCreationService, never()).complete(anyLong(), any());
    }

    @Test
    void 이미_시작된_작업은_건너뛴다() {
        when(characterCreationService.start(5L)).thenReturn(null);

        worker("fake").generate(5L);

        verify(characterCreationService, never()).complete(anyLong(), any());
        verify(characterCreationService, never()).fail(anyLong(), anyString());
    }

    @Test
    void 시작_중_오류도_실패_처리한다() {
        when(characterCreationService.start(5L)).thenThrow(new IllegalStateException("생성 입력 없음"));

        worker("fake").generate(5L);

        verify(characterCreationService).fail(5L, "생성 입력 없음");
    }
}
