package com.nyangmeong.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.auth.dto.AuthMeResponse;
import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.global.security.session.LoginSessionManager;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * [인증 컨트롤러]
 * - GET /api/auth/me
 * - POST /api/auth/refresh, POST /api/auth/logout : 토큰은 쿠키로만 오가고 data 없음
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginSessionManager loginSessionManager;

    @Operation(summary = "내 정보(헤더 프로필·드롭다운)")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponse>> getMe(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(authService.getMe(me.id())));
    }

    @Operation(summary = "Access 토큰 재발급(Refresh 쿠키 회전)")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request, HttpServletResponse response) {
        loginSessionManager.rotate(request, response);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "로그아웃(Refresh 토큰 폐기)")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        loginSessionManager.revoke(request, response);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
