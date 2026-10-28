package com.nyangmeong.global.file;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 파일 시그니처 형식 검사]
 */
class FileExtensionTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

    @Test
    void 확장자에_맞는_시그니처면_통과한다() {
        assertThat(FileExtension.JPG.matchesContent(JPEG)).isTrue();
        assertThat(FileExtension.JPEG.matchesContent(JPEG)).isTrue();
        assertThat(FileExtension.PNG.matchesContent(PNG)).isTrue();
        assertThat(FileExtension.WEBP.matchesContent(WEBP)).isTrue();
        assertThat(FileExtension.PDF.matchesContent(PDF)).isTrue();
    }

    @Test
    void 다른_형식이나_짧은_내용은_거부한다() {
        assertThat(FileExtension.PNG.matchesContent(PDF)).isFalse();
        assertThat(FileExtension.JPG.matchesContent(PNG)).isFalse();
        assertThat(FileExtension.WEBP.matchesContent(new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'}))
                .isFalse();
        assertThat(FileExtension.PNG.matchesContent(new byte[] {(byte) 0x89, 'P'})).isFalse();
        assertThat(FileExtension.JPG.matchesContent(new byte[0])).isFalse();
    }
}
