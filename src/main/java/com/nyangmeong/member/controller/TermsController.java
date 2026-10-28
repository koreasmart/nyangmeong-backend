package com.nyangmeong.member.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.member.dto.TermsResponse;
import com.nyangmeong.member.dto.TermsSummary;
import com.nyangmeong.member.service.TermsService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [약관 컨트롤러]
 * - GET /api/terms, GET /api/terms/{trmsSn}, 로그인 없이 열려 있다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/terms")
public class TermsController {

    private final TermsService termsService;

    @Operation(summary = "사용 중인 약관 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TermsSummary>>> search() {
        return ResponseEntity.ok(ApiResponse.success(termsService.search()));
    }

    @Operation(summary = "약관 전문 조회")
    @GetMapping("/{trmsSn}")
    public ResponseEntity<ApiResponse<TermsResponse>> getTerms(@PathVariable Long trmsSn) {
        return ResponseEntity.ok(ApiResponse.success(termsService.getTerms(trmsSn)));
    }
}
