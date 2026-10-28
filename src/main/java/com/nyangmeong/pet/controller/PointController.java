package com.nyangmeong.pet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.PointResponse;
import com.nyangmeong.pet.service.PointService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [포인트 요약 컨트롤러]
 * - 보유 포인트·이번 달 적립/사용·오늘 미션 (SCR-MYP-020, SCR-MYP-001 요약)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/points")
public class PointController {

    private final PointService pointService;

    @Operation(summary = "포인트 요약")
    @GetMapping
    public ResponseEntity<ApiResponse<PointResponse>> getPoint(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(pointService.getPoint(me.id())));
    }
}
