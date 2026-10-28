package com.nyangmeong.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.pet.domain.CharacterImagePart;
import com.nyangmeong.pet.dto.PixelCharacterResponse;
import com.nyangmeong.pet.service.PixelCharacterService;

/**
 * [검사 - 캐릭터 생성 요청·캐릭터 이미지 API]
 */
@WebMvcTest(controllers = PetCharacterController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PetCharacterControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PixelCharacterService pixelCharacterService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private final MockMultipartFile photo = new MockMultipartFile("photo", "kongi.jpg", "image/jpeg", new byte[] {1});

    @Test
    @WithMockLoginMember
    void 생성_요청은_201과_요청_상태를_돌려준다() throws Exception {
        when(pixelCharacterService.create(eq(1L), eq(10L), eq("key-1"), any()))
                .thenReturn(new PixelCharacterResponse(3L, 10L, 5L, "CHRC0001", false, false, List.of()));

        mockMvc.perform(multipart("/api/pets/10/pixel-characters").file(photo)
                .header("Idempotency-Key", "key-1")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.crtJobSttsCd").value("CHRC0001"))
                .andExpect(jsonPath("$.data.heldItems").isEmpty());
    }

    @Test
    @WithMockLoginMember
    void 사진이_없으면_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(multipart("/api/pets/10/pixel-characters")
                .header("Idempotency-Key", "key-1")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("photo"));
        verify(pixelCharacterService, never()).create(any(), any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 요청_식별값이_없으면_서비스가_IDEMPOTENCY_KEY_REQUIRED() throws Exception {
        when(pixelCharacterService.create(eq(1L), eq(10L), isNull(), any()))
                .thenThrow(new CustomException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED));

        mockMvc.perform(multipart("/api/pets/10/pixel-characters").file(photo)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    @WithMockLoginMember
    void 이미_캐릭터가_있으면_409() throws Exception {
        when(pixelCharacterService.create(eq(1L), eq(10L), eq("key-2"), any()))
                .thenThrow(new CustomException(ErrorCode.PIXEL_CHARACTER_ALREADY_EXISTS));

        mockMvc.perform(multipart("/api/pets/10/pixel-characters").file(photo)
                .header("Idempotency-Key", "key-2")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PIXEL_CHARACTER_ALREADY_EXISTS"));
    }

    @Test
    @WithMockLoginMember
    void 캐릭터_이미지는_PNG_파일_자체다() throws Exception {
        when(pixelCharacterService.getCharacterImage(1L, 10L, CharacterImagePart.FULL))
                .thenReturn(new StoredFile(new byte[] {9, 9}, "image/png", "character/default-cat.png"));

        mockMvc.perform(get("/api/pets/10/character-image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] {9, 9}));
    }

    @Test
    @WithMockLoginMember
    void 얼굴만_요청할_수_있다() throws Exception {
        when(pixelCharacterService.getCharacterImage(1L, 10L, CharacterImagePart.FACE))
                .thenReturn(new StoredFile(new byte[] {1}, "image/png", "face"));

        mockMvc.perform(get("/api/pets/10/character-image").param("part", "face"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1}));
    }

    @Test
    @WithMockLoginMember
    void 알_수_없는_범위는_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(get("/api/pets/10/character-image").param("part", "body"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 양육하지_않는_반려동물은_403() throws Exception {
        when(pixelCharacterService.getCharacterImage(1L, 99L, CharacterImagePart.FULL))
                .thenThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED));

        mockMvc.perform(get("/api/pets/99/character-image"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_ACCESS_DENIED"));
    }
}
