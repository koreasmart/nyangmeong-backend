package com.nyangmeong.pet.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.pet.service.ItemService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [아이템 컨트롤러]
 * - 이미지는 파일 자체 응답 (docs/convention/api.md 보호 파일 다운로드)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "아이템 이미지")
    @GetMapping("/{itemSn}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long itemSn) {
        StoredFile image = itemService.getImage(itemSn);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }
}
