package com.nyangmeong.ai.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.ai.dto.ConversationCreateRequest;
import com.nyangmeong.ai.dto.ConversationMessageCreateRequest;
import com.nyangmeong.ai.dto.ConversationMessageResponse;
import com.nyangmeong.ai.dto.ConversationResponse;
import com.nyangmeong.ai.dto.ConversationSummary;
import com.nyangmeong.ai.service.AiConversationService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [AI 대화 내부 API 컨트롤러]
 * - /internal/ai/conversations : Python AI 서버가 사용자 쿠키를 그대로 넘겨 호출
 * - 회원은 쿠키로 판정, 회원 번호를 요청으로 받지 않음 (docs/api/ai.md)
 */
@RestController
@RequestMapping("/internal/ai/conversations")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService aiConversationService;

    @Operation(summary = "AI 대화 생성(방과 첫 메시지)")
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationSummary>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @RequestBody ConversationCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(aiConversationService.create(me.id(), req)));
    }

    @Operation(summary = "AI 대화 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @RequestParam(required = false) Long petSn) {
        return ResponseEntity.ok(ApiResponse.success(aiConversationService.search(me.id(), petSn)));
    }

    @Operation(summary = "AI 대화 상세 조회")
    @GetMapping("/{aiCnvrsRoomSn}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long aiCnvrsRoomSn) {
        return ResponseEntity.ok(ApiResponse.success(aiConversationService.getConversation(me.id(), aiCnvrsRoomSn)));
    }

    @Operation(summary = "AI 대화 메시지 추가")
    @PostMapping("/{aiCnvrsRoomSn}/messages")
    public ResponseEntity<ApiResponse<ConversationMessageResponse>> createMessage(
            @AuthenticationPrincipal LoginMember me, @PathVariable Long aiCnvrsRoomSn,
            @Valid @RequestBody ConversationMessageCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(aiConversationService.createMessage(me.id(), aiCnvrsRoomSn, req)));
    }

    @Operation(summary = "AI 대화 삭제")
    @DeleteMapping("/{aiCnvrsRoomSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long aiCnvrsRoomSn) {
        aiConversationService.delete(me.id(), aiCnvrsRoomSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
