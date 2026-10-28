package com.nyangmeong.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.auth.dto.DevTokenRequest;
import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.AuthMember;
import com.nyangmeong.global.security.session.LoginSessionManager;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [개발용 토큰 발급]
 * - 로그인 API(YAY) 전까지 회원 번호로 쿠키 발급
 * - prod 프로필 제외, 로그인 구현 후 삭제
 */
@Profile("!prod")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class DevTokenController {

    private final AuthService authService;
    private final LoginSessionManager loginSessionManager;

    @Operation(summary = "[개발용] 회원 번호로 토큰 쿠키 발급")
    @PostMapping("/dev-tokens")
    public ResponseEntity<ApiResponse<Void>> issue(@Valid @RequestBody DevTokenRequest req,
            HttpServletResponse response) {
        AuthMember member = authService.getForAuth(req.mbrSn());
        loginSessionManager.issue(member.getMbrSn(), response);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
