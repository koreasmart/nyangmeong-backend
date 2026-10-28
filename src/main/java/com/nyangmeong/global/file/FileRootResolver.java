package com.nyangmeong.global.file;

import java.nio.file.Path;
import java.util.Locale;

/**
 * [파일 저장 루트 결정]
 * - app.file.root-dir 을 적었으면 항상 그 값
 * - 비었으면 개발 OS별 기본값 : Windows = 프로젝트가 있는 드라이브 루트 아래, Mac = /Users/Shared 아래
 * - 그 밖의 OS 이거나 prod 이면 지정하라는 오류 : uploads 같은 곳으로 조용히 넘어가지 않는다
 * - 시드 생성기(src/seed)도 같은 규칙을 쓴다
 */
public final class FileRootResolver {

    /** 운영 서버 폴더 (prod 프로필의 app.file.root-dir 에 적는다) */
    public static final String RELATIVE_DIR = "home/ggorilab/attachment";

    private static final String MAC_DIR = "/Users/Shared/ggorilab/attachment";

    private FileRootResolver() {
    }

    /**
     * 현재 PC 기준 결정
     */
    public static Path resolve(String configured, boolean prod) {
        return resolve(configured, prod, System.getProperty("os.name"), System.getProperty("user.dir"));
    }

    /**
     * OS 이름과 실행 폴더를 받아 결정 (검사용)
     */
    static Path resolve(String configured, boolean prod, String osName, String userDir) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (!prod && os.startsWith("windows")) {
            return Path.of(driveRoot(userDir) + RELATIVE_DIR).normalize();
        }
        if (!prod && os.startsWith("mac")) {
            return Path.of(MAC_DIR);
        }
        throw new IllegalStateException("app.file.root-dir을 지정하세요 (prod 이거나 Windows·Mac 이 아니면 기본 경로가 없습니다)");
    }

    /** 프로젝트가 있는 드라이브 루트 (C:\ 와 D:\ 에 프로젝트를 두면 각 드라이브 아래에 만든다) */
    private static String driveRoot(String userDir) {
        if (userDir != null && userDir.length() >= 2 && Character.isLetter(userDir.charAt(0)) && userDir.charAt(1) == ':') {
            return userDir.substring(0, 2) + "\\";
        }
        return "C:\\";
    }
}
