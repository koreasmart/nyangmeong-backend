package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 캐릭터 규격·얼굴 자르기·가짜 생성기]
 */
class CharacterSpecTest {

    private CharacterGenerationRequest request() {
        return new CharacterGenerationRequest(new byte[] {1}, "PETC0002", "코리안숏헤어", "고등어", null);
    }

    @Test
    void 가짜_생성기_결과는_규격과_같다() {
        byte[] image = new FakeCharacterGenerator(new CharacterProperties()).generate(request());

        assertThat(CharacterSpec.matches(image)).isTrue();
    }

    @Test
    void 가짜_실패_설정이면_생성기가_예외를_던진다() {
        CharacterProperties properties = new CharacterProperties();
        properties.setGenerator("fake-fail");

        assertThatThrownBy(() -> new FakeCharacterGenerator(properties).generate(request()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 크기가_다르거나_이미지가_아니면_규격_불일치() {
        byte[] small = CharacterSpec.toPng(new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB));

        assertThat(CharacterSpec.matches(small)).isFalse();
        assertThat(CharacterSpec.matches(new byte[] {1, 2, 3})).isFalse();
        assertThat(CharacterSpec.matches(null)).isFalse();
    }

    @Test
    void 얼굴_영역만_잘라낸다() throws Exception {
        byte[] full = CharacterSpec.toPng(new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB));

        BufferedImage face = ImageIO.read(new ByteArrayInputStream(CharacterSpec.cropFace(full)));

        assertThat(face.getWidth()).isEqualTo(CharacterSpec.FACE.width);
        assertThat(face.getHeight()).isEqualTo(CharacterSpec.FACE.height);
    }
}
