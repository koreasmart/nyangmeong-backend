package com.nyangmeong.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.community.dto.CommentResponse;
import com.nyangmeong.community.dto.CommentSummary;
import com.nyangmeong.community.dto.CommunityAiCommentaryResponse;
import com.nyangmeong.community.dto.PostImageSummary;
import com.nyangmeong.community.dto.PostLikeResponse;
import com.nyangmeong.community.dto.PostResponse;
import com.nyangmeong.community.dto.PostSearchCond;
import com.nyangmeong.community.dto.PostSummary;
import com.nyangmeong.community.dto.ReportResponse;
import com.nyangmeong.community.service.CommentService;
import com.nyangmeong.community.service.CommunityAiCommentaryService;
import com.nyangmeong.community.service.PostService;
import com.nyangmeong.community.service.ReportService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

/**
 * [검사 - 펫이야기 게시글·댓글·신고 API]
 */
@WebMvcTest(controllers = { PostController.class, CommentController.class, ReportController.class,
        CommunityAiCommentaryController.class })
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PostControllerTest {

    private static final String AJAX = "XMLHttpRequest";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PostService postService;

    @MockitoBean
    CommentService commentService;

    @MockitoBean
    ReportService reportService;

    @MockitoBean
    CommunityAiCommentaryService commentaryService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private PostResponse response() {
        return new PostResponse(7L, "CMUC0002", "제목", "내용", 1L, "닉네임", LocalDateTime.of(2026, 10, 26, 9, 0), 10,
                3, 2, List.of(new PostImageSummary(101L, 0)), false, true);
    }

    @Test
    @WithMockLoginMember
    void 목록은_게시판_키워드_정렬을_서비스에_넘긴다() throws Exception {
        PostSummary row = new PostSummary();
        row.setPstSn(7L);
        row.setPopular(true);
        when(postService.search(any(PostSearchCond.class), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 10, 1, 1, false));

        mockMvc.perform(get("/api/posts").param("bbsCd", "CMUC0002").param("keyword", "산책").param("sort", "popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].pstSn").value(7))
                .andExpect(jsonPath("$.data.content[0].popular").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/posts")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember
    void 건강팁과_인기_포토는_고정_경로로_받는다() throws Exception {
        PostSummary tip = new PostSummary();
        tip.setPstSn(3L);
        when(postService.getHealthTips()).thenReturn(List.of(tip));
        when(postService.getPopularPhoto(1L)).thenReturn(response());

        mockMvc.perform(get("/api/posts/health-tips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pstSn").value(3));
        mockMvc.perform(get("/api/posts/popular-photo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pstSn").value(7));
    }

    @Test
    @WithMockLoginMember
    void 인기_포토가_없으면_data_없이_200() throws Exception {
        when(postService.getPopularPhoto(1L)).thenReturn(null);

        mockMvc.perform(get("/api/posts/popular-photo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @WithMockLoginMember
    void 상세는_로그인_회원_번호로_조회한다() throws Exception {
        when(postService.getPost(1L, 7L)).thenReturn(response());

        mockMvc.perform(get("/api/posts/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").value(true))
                .andExpect(jsonPath("$.data.imgs[0].atchFileSn").value(101));
    }

    @Test
    @WithMockLoginMember
    void 작성은_201과_게시글을_돌려준다() throws Exception {
        when(postService.create(eq(1L), any())).thenReturn(response());

        mockMvc.perform(multipart("/api/posts")
                .file(new MockMultipartFile("images", "a.png", "image/png", new byte[] {1}))
                .param("bbsCd", "CMUC0002").param("pstTtl", "제목").param("pstCn", "내용")
                .header("X-Requested-With", AJAX))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.pstSn").value(7));
    }

    @Test
    @WithMockLoginMember
    void 제목이_50자를_넘거나_내용이_2000자를_넘으면_400() throws Exception {
        mockMvc.perform(multipart("/api/posts")
                .param("bbsCd", "CMUC0001").param("pstTtl", "가".repeat(51)).param("pstCn", "내용")
                .header("X-Requested-With", AJAX))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("pstTtl"));
        mockMvc.perform(multipart("/api/posts")
                .param("bbsCd", "CMUC0001").param("pstTtl", "제목").param("pstCn", "가".repeat(2001))
                .header("X-Requested-With", AJAX))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("pstCn"));
    }

    @Test
    @WithMockLoginMember
    void 남의_글_수정은_403() throws Exception {
        when(postService.update(eq(1L), eq(7L), any())).thenThrow(new CustomException(ErrorCode.NOT_OWNER));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/posts/7")
                .param("pstTtl", "새 제목").header("X-Requested-With", AJAX))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_OWNER"));
    }

    @Test
    @WithMockLoginMember
    void 삭제는_200이고_data가_없다() throws Exception {
        mockMvc.perform(delete("/api/posts/7").header("X-Requested-With", AJAX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        verify(postService).delete(1L, 7L);
    }

    @Test
    @WithMockLoginMember
    void 좋아요_등록은_현재_좋아요_수를_돌려주고_취소는_data가_없다() throws Exception {
        when(postService.like(1L, 7L)).thenReturn(new PostLikeResponse(7L, 4));

        mockMvc.perform(post("/api/posts/7/likes").header("X-Requested-With", AJAX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rcmdtnCnt").value(4));
        mockMvc.perform(delete("/api/posts/7/likes").header("X-Requested-With", AJAX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        verify(postService).unlike(1L, 7L);
    }

    @Test
    @WithMockLoginMember
    void 댓글_목록과_등록() throws Exception {
        CommentSummary row = new CommentSummary();
        row.setCmntSn(10L);
        row.setDelYn(true);
        when(commentService.search(1L, 7L)).thenReturn(List.of(row));
        when(commentService.create(eq(1L), eq(7L), any()))
                .thenReturn(new CommentResponse(11L, 7L, 10L, "답글", 1L, "닉네임", LocalDateTime.of(2026, 10, 26, 9, 0)));

        mockMvc.perform(get("/api/posts/7/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].delYn").value(true));
        mockMvc.perform(post("/api/posts/7/comments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cmntCn\":\"답글\",\"prntCmntSn\":10}").header("X-Requested-With", AJAX))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.prntCmntSn").value(10));
    }

    @Test
    @WithMockLoginMember
    void 댓글은_500자를_넘으면_400() throws Exception {
        mockMvc.perform(post("/api/posts/7/comments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cmntCn\":\"" + "가".repeat(501) + "\"}").header("X-Requested-With", AJAX))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("cmntCn"));
    }

    @Test
    @WithMockLoginMember
    void 댓글_삭제는_200() throws Exception {
        mockMvc.perform(delete("/api/posts/7/comments/10").header("X-Requested-With", AJAX))
                .andExpect(status().isOk());
        verify(commentService).delete(1L, 7L, 10L);
    }

    @Test
    @WithMockLoginMember
    void 신고는_201과_접수_상태를_돌려준다() throws Exception {
        when(reportService.create(eq(1L), any())).thenReturn(new ReportResponse(55L, "CMUC0015"));

        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pstSn\":7,\"dclrRsnCd\":\"CMUC0009\"}").header("X-Requested-With", AJAX))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dclrPrcsSttsCd").value("CMUC0015"));
    }

    @Test
    @WithMockLoginMember
    void 신고_사유가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pstSn\":7}").header("X-Requested-With", AJAX))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("dclrRsnCd"));
    }

    @Test
    @WithMockLoginMember
    void 오늘의_이야기는_날짜를_받아_조회한다() throws Exception {
        when(commentaryService.getCommentary(LocalDate.of(2026, 10, 26)))
                .thenReturn(new CommunityAiCommentaryResponse(null,
                        LocalDate.of(2026, 10, 26), "기본 문구"));

        mockMvc.perform(get("/api/community-ai-commentaries").param("cmtryCrtrYmd", "2026-10-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cmtryCn").value("기본 문구"))
                .andExpect(jsonPath("$.data.cmntyAiCmtrySn").doesNotExist());
    }
}
