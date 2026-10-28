package com.nyangmeong.expense.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.expense.dto.ReceiptSaveCheckRequest;
import com.nyangmeong.expense.dto.ReceiptSaveCheckResponse;
import com.nyangmeong.expense.dto.ReceiptSaveRequest;
import com.nyangmeong.expense.dto.ReceiptSaveResponse;
import com.nyangmeong.expense.service.ReceiptSaveService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [영수증 확인 후 저장 컨트롤러]
 * - 영수증 인식 결과 확인 모달의 저장 전 확인·일괄 저장 (SCR-AIM-008, 화면은 SCR-HLT-002)
 * - 같은 멱등 요청 ID 의 재전송은 201 이 아니라 200 으로 같은 결과를 돌려준다
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/receipts")
public class ReceiptSaveController {

    private final ReceiptSaveService receiptSaveService;

    @Operation(summary = "영수증 저장 전 확인(가계부 중복·가까운 예정 일정)")
    @PostMapping("/{recptSn}/save-check")
    public ResponseEntity<ApiResponse<ReceiptSaveCheckResponse>> check(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long recptSn, @Valid @RequestBody ReceiptSaveCheckRequest req) {
        return ResponseEntity.ok(ApiResponse.success(receiptSaveService.check(me.id(), recptSn, req)));
    }

    @Operation(summary = "영수증 확인 후 저장(검진기록·가계부 일괄)")
    @PostMapping("/{recptSn}/save")
    public ResponseEntity<ApiResponse<ReceiptSaveResponse>> save(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long recptSn, @Valid @RequestBody ReceiptSaveRequest req) {
        ReceiptSaveService.Saved saved = receiptSaveService.save(me.id(), recptSn, req);
        if (saved.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(saved.response()));
        }
        return ResponseEntity.ok(ApiResponse.success(saved.response()));
    }
}
