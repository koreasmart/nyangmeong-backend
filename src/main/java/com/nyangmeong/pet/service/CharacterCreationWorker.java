package com.nyangmeong.pet.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.config.AsyncConfig;
import com.nyangmeong.global.external.CharacterGenerationRequest;
import com.nyangmeong.global.external.CharacterGenerator;
import com.nyangmeong.global.external.CharacterSpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [캐릭터 백그라운드 생성]
 * - 요청 트랜잭션 커밋 뒤 실행, 모달을 닫아도 계속 (FN-MYP-010-09)
 * - 외부 API 오류·규격 불일치 -> 생성 실패, 화면은 종별 기본 캐릭터 (FN-MYP-010-12·18)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterCreationWorker {

    static final String SPEC_MISMATCH = "캐릭터 규격 불일치";

    private final CharacterCreationService characterCreationService;
    private final CharacterGenerator characterGenerator;

    /**
     * 캐릭터 생성 실행
     */
    @Async(AsyncConfig.CHARACTER_EXECUTOR)
    public void generate(Long charCrtJobSn) {
        try {
            CharacterGenerationRequest request = characterCreationService.start(charCrtJobSn);
            if (request == null) {
                return;
            }
            byte[] image = characterGenerator.generate(request);
            if (!CharacterSpec.matches(image)) {
                characterCreationService.fail(charCrtJobSn, SPEC_MISMATCH);
                return;
            }
            characterCreationService.complete(charCrtJobSn, image);
        } catch (RuntimeException e) {
            log.warn("[Character] 생성 실패 charCrtJobSn={}", charCrtJobSn, e);
            characterCreationService.fail(charCrtJobSn, e.getMessage());
        }
    }
}
