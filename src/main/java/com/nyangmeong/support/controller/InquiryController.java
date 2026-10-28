package com.nyangmeong.support.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.support.dto.InquiryCreateRequest;
import com.nyangmeong.support.dto.InquiryResponse;
import com.nyangmeong.support.dto.InquirySearchCond;
import com.nyangmeong.support.dto.InquirySummary;
import com.nyangmeong.support.dto.InquiryUpdateRequest;
import com.nyangmeong.support.service.InquiryService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [1:1 문의 컨트롤러]
 * - SCR-CSC-003 목록·작성·상세·수정, 작성·수정은 multipart/form-data
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "내 1:1 문의 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InquirySummary>>> search(@AuthenticationPrincipal LoginMember me,
            InquirySearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.search(me.id(), cond, pq)));
    }

    @Operation(summary = "내 1:1 문의 상세")
    @GetMapping("/{otoInqrySn}")
    public ResponseEntity<ApiResponse<InquiryResponse>> getInquiry(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoInqrySn) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getInquiry(me.id(), otoInqrySn)));
    }

    @Operation(summary = "1:1 문의 작성")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InquiryResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute InquiryCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(inquiryService.create(me.id(), req)));
    }

    @Operation(summary = "1:1 문의 수정(미답변 상태만)")
    @PatchMapping(value = "/{otoInqrySn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InquiryResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long otoInqrySn, @Valid @ModelAttribute InquiryUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.update(me.id(), otoInqrySn, req)));
    }
}
