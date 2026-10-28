package com.nyangmeong.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.auth.dto.InternalAuthCheckResponse;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;

/**
 * [서버 간 내부 인증 확인 컨트롤러]
 * - GET /internal/auth/check : 사용자 쿠키를 필터가 판정한 뒤 회원 번호·권한만 돌려준다
 * - 세션 폐기·정지·탈퇴 판정은 JwtAuthenticationFilter 가 끝냄
 */
@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    @Operation(summary = "내부 인증 확인(Python 미들웨어용)")
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<InternalAuthCheckResponse>> check(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(InternalAuthCheckResponse.of(me)));
    }
}
