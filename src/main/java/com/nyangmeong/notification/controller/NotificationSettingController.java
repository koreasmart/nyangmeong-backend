package com.nyangmeong.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.notification.dto.NotificationSettingResponse;
import com.nyangmeong.notification.dto.NotificationSettingUpdateRequest;
import com.nyangmeong.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [알림 설정 컨트롤러]
 * - SCR-COM-008, 회원당 하나인 설정이라 /members/me 아래 (docs/convention/api.md)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/notification-settings")
public class NotificationSettingController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 설정 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> getSetting(
            @AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getSetting(me.id())));
    }

    @Operation(summary = "알림 설정 저장")
    @PutMapping
    public ResponseEntity<ApiResponse<NotificationSettingResponse>> updateSetting(
            @AuthenticationPrincipal LoginMember me, @Valid @RequestBody NotificationSettingUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.updateSetting(me.id(), req)));
    }
}
