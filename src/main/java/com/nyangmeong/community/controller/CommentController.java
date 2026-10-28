package com.nyangmeong.community.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.CommentCreateRequest;
import com.nyangmeong.community.dto.CommentResponse;
import com.nyangmeong.community.dto.CommentSummary;
import com.nyangmeong.community.service.CommentService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [댓글 컨트롤러]
 * - SCR-CMU-001 댓글·대댓글 목록·등록·삭제
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{pstSn}/comments")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글·대댓글 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentSummary>>> search(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn) {
        return ResponseEntity.ok(ApiResponse.success(commentService.search(me.id(), pstSn)));
    }

    @Operation(summary = "댓글·대댓글 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn, @Valid @RequestBody CommentCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(commentService.create(me.id(), pstSn, req)));
    }

    @Operation(summary = "댓글 삭제(작성자, 답글 유지)")
    @DeleteMapping("/{cmntSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn, @PathVariable Long cmntSn) {
        commentService.delete(me.id(), pstSn, cmntSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
