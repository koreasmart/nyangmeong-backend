package com.nyangmeong.community.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.CommunityAiCommentaryResponse;
import com.nyangmeong.community.service.CommunityAiCommentaryService;
import com.nyangmeong.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [오늘의 이야기 컨트롤러]
 * - SCR-CMU-001 우측 레일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community-ai-commentaries")
public class CommunityAiCommentaryController {

    private final CommunityAiCommentaryService commentaryService;

    @Operation(summary = "오늘의 이야기")
    @GetMapping
    public ResponseEntity<ApiResponse<CommunityAiCommentaryResponse>> getCommentary(
            @RequestParam(required = false) LocalDate cmtryCrtrYmd) {
        return ResponseEntity.ok(ApiResponse.success(commentaryService.getCommentary(cmtryCrtrYmd)));
    }
}
