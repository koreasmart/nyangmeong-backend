package com.nyangmeong.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.nyangmeong.global.log.SensitiveDataMasker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * [검사 - 전역 예외 처리]
 * - 보안 필터 제외, 예외 변환만
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandlerTest.ThrowingController.class, GlobalExceptionHandler.class, SensitiveDataMasker.class})
class GlobalExceptionHandlerTest {

    record NameRequest(@NotBlank(message = "이름을 입력해 주세요") String mbrNcnm) {
    }

    enum Kind {
        DOG, CAT
    }

    record KindRequest(Kind kind) {
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/business")
        String business() {
            throw new CustomException(ErrorCode.PET_LIMIT_EXCEEDED);
        }

        @PostMapping("/test/valid")
        String valid(@Valid @RequestBody NameRequest req) {
            return "ok";
        }

        @GetMapping("/test/items/{sn}")
        String item(@PathVariable Long sn) {
            return "ok";
        }

        @GetMapping("/test/kind")
        String kind(KindRequest req) {
            return "ok";
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("user@example.com 때문에 실패");
        }

        @GetMapping("/test/upload")
        String upload() {
            throw new MaxUploadSizeExceededException(10L * 1024 * 1024);
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void CustomException은_ErrorCode의_상태와_code로_나간다() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.httpCode").value(400))
                .andExpect(jsonPath("$.code").value("PET_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value(ErrorCode.PET_LIMIT_EXCEEDED.message()))
                .andExpect(jsonPath("$.path").value("/test/business"));
    }

    @Test
    void 검증_실패는_400_INVALID_INPUT_VALUE와_필드_목록이다() throws Exception {
        mockMvc.perform(post("/test/valid").contentType(MediaType.APPLICATION_JSON).content("{\"mbrNcnm\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("mbrNcnm"))
                .andExpect(jsonPath("$.data[0].message").value("이름을 입력해 주세요"));
    }

    @Test
    void 본문을_읽지_못하면_400_BAD_REQUEST다() throws Exception {
        mockMvc.perform(post("/test/valid").contentType(MediaType.APPLICATION_JSON).content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 지원하지_않는_Content_Type은_415_UNSUPPORTED_MEDIA_TYPE이다() throws Exception {
        mockMvc.perform(post("/test/valid").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void 타입_변환_실패는_Java_문구_대신_사람이_읽는_문구다() throws Exception {
        mockMvc.perform(get("/test/kind").param("kind", "BIRD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("kind"))
                .andExpect(jsonPath("$.data[0].message").value("형식이 올바르지 않습니다."));
    }

    @Test
    void 경로_변수_형식이_틀리면_400_BAD_REQUEST다() throws Exception {
        mockMvc.perform(get("/test/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void 없는_경로는_404_NOT_FOUND다() throws Exception {
        mockMvc.perform(get("/test/nowhere"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 허용하지_않는_메서드는_405다() throws Exception {
        mockMvc.perform(post("/test/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void 업로드_크기_초과는_400_FILE_TOO_LARGE다() throws Exception {
        mockMvc.perform(get("/test/upload"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void 그_밖의_예외는_500이고_내부_메시지를_내보내지_않는다() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.message()));
    }
}
