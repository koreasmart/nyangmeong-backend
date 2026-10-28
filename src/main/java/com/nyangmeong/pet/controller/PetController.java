package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.PetCreateRequest;
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.dto.PetUpdateRequest;
import com.nyangmeong.pet.service.PetService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [반려동물 컨트롤러]
 * - 내 반려동물 목록 (SCR-MYP-001 카드, SCR-MYP-002 슬라이드)
 * - 등록·수정·상세 (SCR-MYP-003), 대표 지정 (FN-MYP-010-05)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    @Operation(summary = "내 반려동물 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PetSummary>>> searchMyPets(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(petService.searchMyPets(me.id())));
    }

    @Operation(summary = "반려동물 상세")
    @GetMapping("/{petSn}")
    public ResponseEntity<ApiResponse<PetResponse>> getPet(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn) {
        return ResponseEntity.ok(ApiResponse.success(petService.getPet(me.id(), petSn)));
    }

    @Operation(summary = "반려동물 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<PetResponse>> createPet(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody PetCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(petService.createPet(me.id(), req)));
    }

    @Operation(summary = "반려동물 수정")
    @PatchMapping("/{petSn}")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn, @Valid @RequestBody PetUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(petService.updatePet(me.id(), petSn, req)));
    }

    @Operation(summary = "대표 반려동물 지정")
    @PostMapping("/{petSn}/representative")
    public ResponseEntity<ApiResponse<Void>> designateRepresentative(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long petSn) {
        petService.designateRepresentative(me.id(), petSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
