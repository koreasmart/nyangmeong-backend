package com.nyangmeong.community.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.community.dto.PostCreateRequest;
import com.nyangmeong.community.dto.PostLikeResponse;
import com.nyangmeong.community.dto.PostResponse;
import com.nyangmeong.community.dto.PostSearchCond;
import com.nyangmeong.community.dto.PostSummary;
import com.nyangmeong.community.dto.PostUpdateRequest;
import com.nyangmeong.community.service.PostService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [펫이야기 게시글 컨트롤러]
 * - SCR-CMU-001 목록·상세·작성·수정·삭제·좋아요·인기 포토·건강팁, 작성·수정은 multipart/form-data
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @Operation(summary = "펫이야기 게시글 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostSummary>>> search(PostSearchCond cond, PageQuery pq) {
        return ResponseEntity.ok(ApiResponse.success(postService.search(cond, pq)));
    }

    @Operation(summary = "오늘의 건강팁")
    @GetMapping("/health-tips")
    public ResponseEntity<ApiResponse<List<PostSummary>>> getHealthTips() {
        return ResponseEntity.ok(ApiResponse.success(postService.getHealthTips()));
    }

    @Operation(summary = "인기 포토")
    @GetMapping("/popular-photo")
    public ResponseEntity<ApiResponse<PostResponse>> getPopularPhoto(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPopularPhoto(me.id())));
    }

    @Operation(summary = "게시글 상세")
    @GetMapping("/{pstSn}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(me.id(), pstSn)));
    }

    @Operation(summary = "게시글 작성")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> create(@AuthenticationPrincipal LoginMember me,
            @Valid @ModelAttribute PostCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(postService.create(me.id(), req)));
    }

    @Operation(summary = "게시글 수정(작성자)")
    @PatchMapping(value = "/{pstSn}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> update(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn, @Valid @ModelAttribute PostUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(postService.update(me.id(), pstSn, req)));
    }

    @Operation(summary = "게시글 삭제(작성자)")
    @DeleteMapping("/{pstSn}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn) {
        postService.delete(me.id(), pstSn);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "좋아요 등록")
    @PostMapping("/{pstSn}/likes")
    public ResponseEntity<ApiResponse<PostLikeResponse>> like(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn) {
        return ResponseEntity.ok(ApiResponse.success(postService.like(me.id(), pstSn)));
    }

    @Operation(summary = "좋아요 취소")
    @DeleteMapping("/{pstSn}/likes")
    public ResponseEntity<ApiResponse<Void>> unlike(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pstSn) {
        postService.unlike(me.id(), pstSn);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
