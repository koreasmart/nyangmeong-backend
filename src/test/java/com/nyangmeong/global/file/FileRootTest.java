package com.nyangmeong.global.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

/**
 * [검사 - 파일 저장 루트 시작 확인]
 */
class FileRootTest {

    @TempDir
    Path temp;

    private FileProperties properties(Path dir) {
        FileProperties properties = new FileProperties();
        properties.setRootDir(dir.toString());
        return properties;
    }

    @Test
    void 시작할_때_폴더를_만들고_해석한_경로를_가진다() {
        Path dir = temp.resolve("a/b/attachment");

        FileRoot root = new FileRoot(properties(dir), new MockEnvironment());

        assertThat(Files.isDirectory(dir)).isTrue();
        assertThat(root.getPath()).isEqualTo(dir.toAbsolutePath().normalize());
    }

    @Test
    void 쓸_수_없으면_prod는_앱을_띄우지_않는다() throws Exception {
        Path file = Files.createFile(temp.resolve("not-a-dir"));
        FileProperties properties = properties(file);
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThatThrownBy(() -> new FileRoot(properties, prod))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("쓸 수 없습니다");
    }

    @Test
    void 쓸_수_없어도_개발은_경고만_하고_뜬다() throws Exception {
        Path file = Files.createFile(temp.resolve("not-a-dir"));

        FileRoot root = new FileRoot(properties(file), new MockEnvironment());

        assertThat(root.getPath()).isEqualTo(file.toAbsolutePath().normalize());
    }

    @Test
    void prod에_키가_없으면_시작_실패() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThatThrownBy(() -> new FileRoot(new FileProperties(), prod))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("app.file.root-dir을 지정하세요");
    }
}
