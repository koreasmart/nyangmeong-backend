package com.nyangmeong.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 파일 저장 루트 결정]
 */
class FileRootResolverTest {

    private static boolean windows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    @Test
    void 명시한_값이_항상_우선한다() {
        Path expected = Path.of("custom-dir").toAbsolutePath().normalize();

        assertThat(FileRootResolver.resolve("custom-dir", false, "Windows 11", "C:\\work")).isEqualTo(expected);
        assertThat(FileRootResolver.resolve("custom-dir", true, "Linux", "/srv/app")).isEqualTo(expected);
    }

    @Test
    void Windows는_프로젝트가_있는_드라이브_루트_아래에_만든다() {
        assumeTrue(windows());

        assertThat(FileRootResolver.resolve("", false, "Windows 11", "C:\\work\\backend"))
                .isEqualTo(Path.of("C:\\home\\ggorilab\\attachment"));
        assertThat(FileRootResolver.resolve(null, false, "Windows 11", "D:\\proj\\backend"))
                .isEqualTo(Path.of("D:\\home\\ggorilab\\attachment"));
    }

    @Test
    void Mac은_Users_Shared_아래에_만든다() {
        assertThat(FileRootResolver.resolve(" ", false, "Mac OS X", "/Users/a/backend"))
                .isEqualTo(Path.of("/Users/Shared/ggorilab/attachment"));
    }

    @Test
    void Windows와_Mac이_아니면_키가_없을_때_지정하라는_오류() {
        assertThatThrownBy(() -> FileRootResolver.resolve(null, false, "Linux", "/srv/app"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("app.file.root-dir을 지정하세요");
    }

    @Test
    void prod는_키가_없으면_OS와_관계없이_오류() {
        assertThatThrownBy(() -> FileRootResolver.resolve(null, true, "Windows 11", "C:\\work"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("app.file.root-dir을 지정하세요");
        assertThatThrownBy(() -> FileRootResolver.resolve("", true, "Mac OS X", "/Users/a"))
                .isInstanceOf(IllegalStateException.class);
    }
}
