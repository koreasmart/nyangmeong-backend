package com.nyangmeong.pet.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.DailyMissionSummary;
import com.nyangmeong.pet.service.CareMissionService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [케어미션 컨트롤러]
 * - 반려동물별 그날의 케어미션, 주양육자만 (FN-MYP-020-03·04), msnCrtrYmd 없으면 오늘
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/daily-missions")
public class CareMissionController {

    private final CareMissionService careMissionService;

    @Operation(summary = "그날의 케어미션")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyMissionSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @RequestParam(required = false) LocalDate msnCrtrYmd) {
        return ResponseEntity.ok(ApiResponse.success(careMissionService.search(me.id(), petSn, msnCrtrYmd)));
    }
}
