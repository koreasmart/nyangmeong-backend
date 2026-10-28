package com.nyangmeong.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.ReportCreateRequest;
import com.nyangmeong.community.dto.ReportResponse;
import com.nyangmeong.community.service.ReportService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [신고 컨트롤러]
 * - 게시글·댓글 신고, 펫이야기·실종센터·펫돌봄 공통
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "게시글·댓글 신고")
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody ReportCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(reportService.create(me.id(), req)));
    }
}
