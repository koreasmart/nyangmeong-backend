package com.nyangmeong.global.file;

import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [공개 파일 컨트롤러]
 * - GET /api/files/public/{atchFileSn} : 로그인 없이 연다 (게시글 사진·캐릭터 이미지)
 * - 비공개 파일(영수증 등)은 번호를 알아도 404 : 각 도메인 API 가 소유 검사 후 준다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileStore fileStore;

    @Operation(summary = "공개 이미지 파일")
    @GetMapping("/public/{atchFileSn}")
    public ResponseEntity<byte[]> getPublic(@PathVariable Long atchFileSn) {
        StoredFile file = fileStore.loadPublic(atchFileSn);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
