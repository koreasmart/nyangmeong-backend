package com.nyangmeong.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.auth.dto.CaptchaCreateRequest;
import com.nyangmeong.auth.dto.CaptchaResponse;
import com.nyangmeong.auth.dto.LoginAttemptLimitQuery;
import com.nyangmeong.auth.dto.LoginAttemptLimitResponse;
import com.nyangmeong.auth.dto.LoginRequest;
import com.nyangmeong.auth.dto.LoginResult;
import com.nyangmeong.auth.service.LoginService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.session.LoginSessionManager;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [이메일 로그인 컨트롤러]
 * - GET /api/auth/login-attempt-limits, POST /api/auth/captchas, POST /api/auth/login
 * - 로그인 실패는 4xx 이고 data 에 실패 횟수·보안코드 요구 상태가 실린다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthLoginController {

    private final LoginService loginService;
    private final LoginSessionManager loginSessionManager;

    @Operation(summary = "아이디 기준 로그인 실패 횟수·보안코드 요구 상태")
    @GetMapping("/login-attempt-limits")
    public ResponseEntity<ApiResponse<LoginAttemptLimitResponse>> getAttemptLimit(
            @Valid LoginAttemptLimitQuery query) {
        return ResponseEntity.ok(ApiResponse.success(loginService.getAttemptLimit(query.email())));
    }

    @Operation(summary = "보안코드(캡챠) 발급·새로고침")
    @PostMapping("/captchas")
    public ResponseEntity<ApiResponse<CaptchaResponse>> createCaptcha(@Valid @RequestBody CaptchaCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(loginService.issueCaptcha(req)));
    }

    @Operation(summary = "이메일 로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request,
            HttpServletResponse response) {
        LoginResult result = loginService.login(req);
        if (!result.succeeded()) {
            return ResponseEntity.status(result.error().status())
                    .<ApiResponse<?>>body(ApiResponse.errorWithData(result.error(), request.getRequestURI(), result.limits()));
        }
        loginSessionManager.issue(result.mbrSn(), response);
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }
}
