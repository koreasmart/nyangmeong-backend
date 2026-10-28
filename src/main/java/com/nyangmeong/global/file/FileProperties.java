package com.nyangmeong.global.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [파일 저장 설정]
 * - app.file.root-dir, 비우면 OS별 기본 경로(FileRootResolver)
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.file")
public class FileProperties {

    /** 저장 루트 폴더 (기본값 없음) */
    private String rootDir;
}
