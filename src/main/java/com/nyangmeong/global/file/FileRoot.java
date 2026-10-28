package com.nyangmeong.global.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * [파일 저장 루트]
 * - 시작할 때 폴더 생성과 쓰기 확인, 해석한 절대 경로를 로그 한 줄로 남긴다
 * - 쓸 수 없으면 prod 는 앱을 띄우지 않고 그 밖은 경고만 한다
 * - 저장(FileStore)과 읽기가 같은 값을 쓴다
 */
@Slf4j
@Component
public class FileRoot {

    private final Path path;

    @Autowired
    public FileRoot(FileProperties fileProperties, Environment environment) {
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        this.path = FileRootResolver.resolve(fileProperties.getRootDir(), prod);
        log.info("[FileRoot] 파일 저장 루트={}", path);
        String problem = checkWritable(path);
        if (problem != null) {
            if (prod) {
                throw new IllegalStateException("파일 저장 루트에 쓸 수 없습니다 path=" + path + " : " + problem);
            }
            log.warn("[FileRoot] 파일 저장 루트에 쓸 수 없습니다 path={} : {}", path, problem);
        }
    }

    /** 검사용 : 해석·확인 없이 경로만 받는다 */
    public FileRoot(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public Path getPath() {
        return path;
    }

    /**
     * 폴더 생성과 쓰기 확인
     * @return 문제가 없으면 null, 있으면 이유
     */
    static String checkWritable(Path root) {
        try {
            Files.createDirectories(root);
            Path probe = Files.createTempFile(root, ".write-check", ".tmp");
            Files.deleteIfExists(probe);
            return null;
        } catch (IOException | RuntimeException e) {
            return e.toString();
        }
    }
}
