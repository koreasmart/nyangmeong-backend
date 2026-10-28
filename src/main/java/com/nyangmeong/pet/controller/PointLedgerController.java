package com.nyangmeong.pet.controller;

import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.PointLedgerSummary;
import com.nyangmeong.pet.service.PointService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [포인트 내역 컨트롤러]
 * - 선택한 월의 적립·사용 내역 (FN-MYP-020-02), yearMonth = yyyy-MM, 없으면 이번 달
 * - 형식이 틀린 yearMonth : 변환 실패 -> GlobalExceptionHandler 400
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/point-ledgers")
public class PointLedgerController {

    private final PointService pointService;

    @Operation(summary = "월 포인트 내역")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PointLedgerSummary>>> searchLedgers(
            @AuthenticationPrincipal LoginMember me,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        return ResponseEntity.ok(ApiResponse.success(pointService.searchLedgers(me.id(), yearMonth)));
    }
}
