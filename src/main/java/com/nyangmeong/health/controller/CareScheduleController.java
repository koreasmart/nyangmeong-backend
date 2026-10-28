package com.nyangmeong.health.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.health.dto.CareScheduleCreateRequest;
import com.nyangmeong.health.dto.CareScheduleResponse;
import com.nyangmeong.health.dto.CareScheduleSearchCond;
import com.nyangmeong.health.dto.CareScheduleSummary;
import com.nyangmeong.health.dto.CareScheduleUpdateRequest;
import com.nyangmeong.health.service.CareScheduleService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [케어 캘린더 일정 컨트롤러]
 * - SCR-HLT-003 일정, AI 대화 일정 저장도 같은 등록 API (docs/api/health.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets/{petSn}/care-schedules")
public class CareScheduleController {

    private final CareScheduleService careScheduleService;

    @Operation(summary = "케어 캘린더 일정 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CareScheduleSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @ModelAttribute CareScheduleSearchCond cond) {
        return ResponseEntity.ok(ApiResponse.success(careScheduleService.search(me.id(), petSn, cond)));
    }

    @Operation(summary = "일정 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<CareScheduleResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody CareScheduleCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(careScheduleService.create(me.id(), petSn, req)));
    }

    @Operation(summary = "일정 수정")
    @PatchMapping("/{schdlSn}")
    public ResponseEntity<ApiResponse<CareScheduleResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long schdlSn, @Valid @RequestBody CareScheduleUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(careScheduleService.update(me.id(), petSn, schdlSn, req)));
    }

    @Operation(summary = "일정 삭제")
    @DeleteMapping("/{schdlSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @PathVariable Long schdlSn) {
        careScheduleService.delete(me.id(), petSn, schdlSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
