package com.nyangmeong.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.notification.dto.NotificationListRequest;
import com.nyangmeong.notification.dto.NotificationSummary;
import com.nyangmeong.notification.dto.UnreadCountResponse;
import com.nyangmeong.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [알림 드롭다운 컨트롤러]
 * - SCR-COM-004, 헤더 배지(SCR-COM-002). 알림 설정은 NotificationSettingController
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "미읽음 알림 개수(헤더 배지)")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(me.id())));
    }

    @Operation(summary = "알림 드롭다운 최근 5건(탭별)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute NotificationListRequest req) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.search(me.id(), req.tab())));
    }

    @Operation(summary = "알림 읽음 처리")
    @PostMapping("/{inappNotiSn}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long inappNotiSn) {
        notificationService.markRead(me.id(), inappNotiSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
