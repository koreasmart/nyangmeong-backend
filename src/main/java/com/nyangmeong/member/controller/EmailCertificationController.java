package com.nyangmeong.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.member.dto.EmailCertificationCreateRequest;
import com.nyangmeong.member.dto.EmailCertificationResponse;
import com.nyangmeong.member.dto.EmailCertificationResult;
import com.nyangmeong.member.dto.EmailCertificationVerifyRequest;
import com.nyangmeong.member.service.EmailCertificationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [이메일 인증 컨트롤러]
 * - POST /api/email-certifications, POST /api/email-certifications/{emlCertSn}/verify, 로그인 없이 열려 있다
 * - 재전송 제한은 4xx 이고 data 에 재시도 가능 시각이 실린다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email-certifications")
public class EmailCertificationController {

    private final EmailCertificationService emailCertificationService;

    @Operation(summary = "이메일 인증번호 발송·재전송")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> send(@Valid @RequestBody EmailCertificationCreateRequest req,
            HttpServletRequest request) {
        EmailCertificationResult result = emailCertificationService.send(req);
        if (!result.succeeded()) {
            return ResponseEntity.status(result.error().status())
                    .<ApiResponse<?>>body(ApiResponse.errorWithData(result.error(), request.getRequestURI(),
                            result.response()));
        }
        return ResponseEntity.ok(ApiResponse.success(result.response()));
    }

    @Operation(summary = "이메일 인증번호 확인")
    @PostMapping("/{emlCertSn}/verify")
    public ResponseEntity<ApiResponse<EmailCertificationResponse>> verify(@PathVariable Long emlCertSn,
            @Valid @RequestBody EmailCertificationVerifyRequest req) {
        return ResponseEntity.ok(ApiResponse.success(emailCertificationService.verify(emlCertSn, req)));
    }
}
