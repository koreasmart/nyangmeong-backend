package com.nyangmeong.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.global.error.GlobalExceptionHandler;
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
import com.nyangmeong.pet.dto.PetResponse;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 반려동물 목록·상세·등록·수정·대표 지정 API]
 */
@WebMvcTest(controllers = PetController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PetService petService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401() throws Exception {
        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpCode").value(401));
    }

    @Test
    @WithMockLoginMember
    void 목록은_역할_대표_인원과_상세_항목을_돌려준다() throws Exception {
        PetSummary pet = new PetSummary();
        pet.setPetSn(10L);
        pet.setPetNm("뭉치");
        pet.setPetSeCd("PETC0001");
        pet.setVrtyDrctInptNm("믹스");
        pet.setPetGndrCd("PETC0003");
        pet.setPetBrdt(LocalDate.of(2023, 5, 1));
        pet.setPetBdwg(new BigDecimal("4.250"));
        pet.setColrCd("PETC0099");
        pet.setColrDrctInptNm("연갈색");
        pet.setAlgyCn("닭고기,연어");
        pet.setDssCn("슬개골 탈구");
        pet.setBrupRoleCd("PETC0018");
        pet.setRprsPetYn(true);
        pet.setCaregiverCount(2);
        pet.setBscCharSbstYn(true);
        when(petService.searchMyPets(1L)).thenReturn(List.of(pet));

        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200))
                .andExpect(jsonPath("$.data[0].petSn").value(10))
                .andExpect(jsonPath("$.data[0].vrtySn").doesNotExist())
                .andExpect(jsonPath("$.data[0].vrtyDrctInptNm").value("믹스"))
                .andExpect(jsonPath("$.data[0].colrDrctInptNm").value("연갈색"))
                .andExpect(jsonPath("$.data[0].algyCn").value("닭고기,연어"))
                .andExpect(jsonPath("$.data[0].dssCn").value("슬개골 탈구"))
                .andExpect(jsonPath("$.data[0].brupRoleCd").value("PETC0018"))
                .andExpect(jsonPath("$.data[0].rprsPetYn").value(true))
                .andExpect(jsonPath("$.data[0].caregiverCount").value(2))
                .andExpect(jsonPath("$.data[0].bscCharSbstYn").value(true));
    }

    @Test
    @WithMockLoginMember
    void 상세는_모달_값을_주고_체중_잠금_필드는_없다() throws Exception {
        PetResponse res = new PetResponse(10L, "뭉치", "PETC0001", 15L, "푸들", null, "PETC0003",
                LocalDate.of(2023, 5, 1), new BigDecimal("4.250"), "PETC0006", null, null, null, null, null,
                "PETC0018", true, true, null, new BigDecimal("3"), new BigDecimal("5"), List.of());
        when(petService.getPet(1L, 10L)).thenReturn(res);

        mockMvc.perform(get("/api/pets/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vrtyNm").value("푸들"))
                .andExpect(jsonPath("$.data.nutrYn").value(true))
                .andExpect(jsonPath("$.data.minBdwg").value(3))
                .andExpect(jsonPath("$.data.bdwgEditableYn").doesNotExist())
                .andExpect(jsonPath("$.data.latestBdwgRcdDt").doesNotExist());
    }

    @Test
    @WithMockLoginMember
    void 등록은_201() throws Exception {
        when(petService.createPet(eq(1L), any())).thenReturn(null);

        mockMvc.perform(json(post("/api/pets"), createBody("뭉치", "4.25", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201));
    }

    @Test
    @WithMockLoginMember
    void 이름이_21자면_400() throws Exception {
        mockMvc.perform(json(post("/api/pets"), createBody("가".repeat(21), "4.25", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(petService, never()).createPet(anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 체중이_0이거나_소수_넷째_자리면_400() throws Exception {
        mockMvc.perform(json(post("/api/pets"), createBody("뭉치", "0", "")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post("/api/pets"), createBody("뭉치", "4.2501", "")))
                .andExpect(status().isBadRequest());
        verify(petService, never()).createPet(anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 칩_번호에_문자가_섞이면_400() throws Exception {
        mockMvc.perform(json(post("/api/pets"), createBody("뭉치", "4.25", "41000A")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(patch("/api/pets/10"), "{\"chipNo\":\"41000A\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 수정은_200() throws Exception {
        when(petService.updatePet(eq(1L), eq(10L), any())).thenReturn(null);

        mockMvc.perform(json(patch("/api/pets/10"), "{\"petBdwg\":6.5,\"chipNo\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200));
    }

    @Test
    @WithMockLoginMember
    void 대표_지정은_200() throws Exception {
        mockMvc.perform(post("/api/pets/10/representative").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200));
        verify(petService).designateRepresentative(1L, 10L);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String createBody(String petNm, String petBdwg, String chipNo) {
        return """
                {"petNm":"%s","petSeCd":"PETC0001","vrtySn":15,"petBrdt":"2023-05-01","chipNo":"%s",
                 "petGndrCd":"PETC0003","nutrYn":true,"petBdwg":%s,"minBdwg":3,"maxBdwg":5,"colrCd":"PETC0006"}
                """.formatted(petNm, chipNo, petBdwg);
    }
}
