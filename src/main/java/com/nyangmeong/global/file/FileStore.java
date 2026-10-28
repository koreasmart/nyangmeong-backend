package com.nyangmeong.global.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [파일 저장·읽기]
 * - 디스크(FileRoot) + ATCH_FILE 행, 확장자 jpg·jpeg·png·webp + 실제 형식, 크기 상한 SYS_STNG.FILE_MAX_SZ
 * - 공개 파일은 public/ 아래에 저장하고(storePublic) 로그인 없이 열린다. 나머지는 비공개이며 각 도메인 API 가 소유 검사 후 준다
 * - 트랜잭션 롤백 시 디스크 파일도 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStore {

    /** 공개 파일의 저장 경로 접두어 (ATCH_FILE.PHYS_STRG_PATH 는 루트를 뺀 상대 경로) */
    public static final String PUBLIC_PREFIX = "public/";

    private static final int ORGNL_FILE_NM_MAX = 200;
    private static final DateTimeFormatter DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final AtchFileMapper atchFileMapper;
    private final FileRoot fileRoot;
    private final SystemSettingService systemSettingService;

    /**
     * 업로드 이미지 저장 (비공개)
     */
    public Long store(MultipartFile file, String actorId) {
        return store(file, actorId, false);
    }

    /**
     * 업로드 이미지 저장 (공개 : GET /api/files/public/{atchFileSn})
     */
    public Long storePublic(MultipartFile file, String actorId) {
        return store(file, actorId, true);
    }

    private Long store(MultipartFile file, String actorId, boolean publicFile) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        FileExtension extension = FileExtension.ofFileName(file.getOriginalFilename())
                .filter(FileExtension::isImage)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED));
        if (file.getSize() > systemSettingService.get().getFileMaxSz()) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!extension.matchesContent(content)) {
            throw new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        return save(content, extension, file.getOriginalFilename(), actorId, publicFile);
    }

    /**
     * 서버 생성 파일 저장 (비공개)
     */
    public Long store(byte[] content, FileExtension extension, String orgnlFileNm, String actorId) {
        return save(content, extension, orgnlFileNm, actorId, false);
    }

    /**
     * 서버 생성 파일 저장 (공개)
     */
    public Long storePublic(byte[] content, FileExtension extension, String orgnlFileNm, String actorId) {
        return save(content, extension, orgnlFileNm, actorId, true);
    }

    /**
     * 파일 읽기
     */
    public StoredFile load(Long atchFileSn) {
        return read(atchFileMapper.selectInUse(atchFileSn));
    }

    /**
     * 공개 파일 읽기 : 공개가 아닌 파일은 없는 파일과 똑같이 FILE_NOT_FOUND
     */
    public StoredFile loadPublic(Long atchFileSn) {
        AtchFile atchFile = atchFileMapper.selectInUse(atchFileSn);
        if (atchFile == null || !atchFile.getPhysStrgPath().startsWith(PUBLIC_PREFIX)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return read(atchFile);
    }

    private StoredFile read(AtchFile atchFile) {
        if (atchFile == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        Path path = resolve(atchFile.getPhysStrgPath());
        if (!Files.isRegularFile(path)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            return new StoredFile(Files.readAllBytes(path),
                    FileExtension.from(atchFile.getFileExtnCd()).getContentType(), atchFile.getOrgnlFileNm());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 디스크 저장과 ATCH_FILE 등록
     */
    private Long save(byte[] content, FileExtension extension, String orgnlFileNm, String actorId, boolean publicFile) {
        String physStrgPath = (publicFile ? PUBLIC_PREFIX : "") + LocalDate.now().format(DIR_FORMAT) + "/"
                + UUID.randomUUID() + "." + extension.getExtension();
        Path path = resolve(physStrgPath);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        deleteOnRollback(path);

        AtchFile atchFile = new AtchFile();
        atchFile.setFileExtnCd(extension.getCode());
        atchFile.setOrgnlFileNm(truncate(orgnlFileNm));
        atchFile.setPhysStrgPath(physStrgPath);
        atchFile.setFileSz((long) content.length);
        atchFile.setRgtrId(actorId);
        atchFileMapper.insert(atchFile);
        return atchFile.getAtchFileSn();
    }

    /**
     * 저장 경로 계산
     * - 루트 밖 경로 차단
     */
    private Path resolve(String physStrgPath) {
        Path root = fileRoot.getPath();
        Path path = root.resolve(physStrgPath).normalize();
        if (!path.startsWith(root)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return path;
    }

    /**
     * 롤백 시 파일 삭제 등록
     */
    private void deleteOnRollback(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        log.warn("[FileStore] 롤백 파일 삭제 실패 path={}", path, e);
                    }
                }
            }
        });
    }

    /**
     * 원본 파일명 길이 제한
     */
    private String truncate(String orgnlFileNm) {
        String name = orgnlFileNm == null || orgnlFileNm.isBlank() ? "file" : orgnlFileNm;
        return name.length() > ORGNL_FILE_NM_MAX ? name.substring(name.length() - ORGNL_FILE_NM_MAX) : name;
    }
}
