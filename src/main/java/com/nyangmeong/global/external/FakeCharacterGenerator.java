package com.nyangmeong.global.external;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * [가짜 캐릭터 생성기]
 * - 외부 API 결정 전 개발용 : 사진과 무관하게 규격 PNG 반환
 * - app.character.generator=fake-fail -> 생성 실패
 */
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${app.character.generator:fake}'.startsWith('fake')")
public class FakeCharacterGenerator implements CharacterGenerator {

    private static final String FAIL = "fake-fail";

    private final CharacterProperties characterProperties;

    @Override
    public byte[] generate(CharacterGenerationRequest request) {
        if (FAIL.equals(characterProperties.getGenerator())) {
            throw new IllegalStateException("가짜 캐릭터 생성 실패");
        }
        int size = CharacterSpec.CANVAS_SIZE;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(0xF2A65A));
        g.fillRect(160, 300, 192, 160);
        g.fillRect(CharacterSpec.FACE.x + 32, CharacterSpec.FACE.y + 32, CharacterSpec.FACE.width - 64,
                CharacterSpec.FACE.height - 64);
        g.setColor(Color.BLACK);
        g.fillRect(208, 176, 24, 24);
        g.fillRect(280, 176, 24, 24);
        g.dispose();
        return CharacterSpec.toPng(image);
    }
}
