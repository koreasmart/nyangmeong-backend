package com.nyangmeong.expense.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.expense.dto.ReceiptAnalyzeRequest;
import com.nyangmeong.expense.dto.ReceiptCreateRequest;
import com.nyangmeong.expense.dto.ReceiptResponse;
import com.nyangmeong.expense.service.ReceiptAnalyzer;
import com.nyangmeong.expense.service.ReceiptService;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [영수증 컨트롤러]
 * - 검진 경로 영수증 업로드·OCR 분석·취소 (SCR-AIM-008, 확인 화면은 SCR-HLT-002)
 * - 분석 실패도 200·201 : 상태는 ocrPrcsSttsCd 로 구분
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptAnalyzer receiptAnalyzer;
    private final ReceiptService receiptService;

    @Operation(summary = "영수증 업로드와 OCR 분석")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReceiptResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute ReceiptCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(receiptAnalyzer.create(me.id(), req.petSn(), req.file())));
    }

    @Operation(summary = "영수증 원본 이미지")
    @GetMapping("/{recptSn}/image")
    public ResponseEntity<byte[]> getImage(@AuthenticationPrincipal LoginMember me, @PathVariable Long recptSn) {
        StoredFile image = receiptService.getImage(me.id(), recptSn);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }

    @Operation(summary = "영수증 다시 분석(취소·인식 실패·일시 오류 뒤)")
    @PostMapping("/{recptSn}/analyze")
    public ResponseEntity<ApiResponse<ReceiptResponse>> analyze(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long recptSn, @Valid @RequestBody ReceiptAnalyzeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(receiptAnalyzer.analyze(me.id(), recptSn, req.petSn())));
    }

    @Operation(summary = "영수증 분석 취소")
    @PostMapping("/{recptSn}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long recptSn) {
        receiptService.cancel(me.id(), recptSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
