package com.nyangmeong.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
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
import com.nyangmeong.pet.dto.EquippedItemSaveRequest;
import com.nyangmeong.pet.dto.ItemPurchaseRequest;
import com.nyangmeong.pet.dto.HeldItemSummary;
import com.nyangmeong.pet.dto.PixelCharacterResponse;
import com.nyangmeong.pet.dto.PixelCharacterSummary;
import com.nyangmeong.pet.dto.ShopItemListResponse;
import com.nyangmeong.pet.dto.ShopItemSummary;
import com.nyangmeong.pet.service.ItemService;
import com.nyangmeong.pet.service.PixelCharacterService;

/**
 * [검사 - 픽셀 캐릭터 목록·꾸미기·아이템 상점 API]
 */
@WebMvcTest(controllers = PixelCharacterController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PixelCharacterControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PixelCharacterService pixelCharacterService;

    @MockitoBean
    ItemService itemService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private PixelCharacterSummary row(Long petSn, String brupRoleCd, Long pixelCharSn, Boolean bscCharSbstYn) {
        PixelCharacterSummary row = new PixelCharacterSummary();
        row.setPetSn(petSn);
        row.setPetNm("콩이");
        row.setPetSeCd("PETC0002");
        row.setBrupRoleCd(brupRoleCd);
        row.setRprsPetYn(petSn == 10L);
        row.setPixelCharSn(pixelCharSn);
        row.setBscCharSbstYn(bscCharSbstYn);
        return row;
    }

    private PixelCharacterResponse response() {
        HeldItemSummary item = new HeldItemSummary();
        item.setHldItemSn(100L);
        item.setItemSn(1L);
        item.setItemNm("리본");
        item.setItemRegnCd("MYPC0001");
        item.setAtchFileSn(40L);
        item.setEquipYn(true);
        return new PixelCharacterResponse(3L, 10L, 5L, "CHRC0003", false, false, List.of(item));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get("/api/pixel-characters"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember
    void 주양육_2건과_공동양육_1건을_돌려준다() throws Exception {
        when(pixelCharacterService.search(1L)).thenReturn(List.of(
                row(10L, "PETC0018", 3L, false),
                row(11L, "PETC0018", null, null),
                row(12L, "PETC0019", 4L, true)));

        mockMvc.perform(get("/api/pixel-characters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].rprsPetYn").value(true))
                .andExpect(jsonPath("$.data[1].pixelCharSn").doesNotExist())
                .andExpect(jsonPath("$.data[2].brupRoleCd").value("PETC0019"))
                .andExpect(jsonPath("$.data[2].bscCharSbstYn").value(true));
    }

    @Test
    @WithMockLoginMember
    void 꾸미기_상세에_보관함이_있다() throws Exception {
        when(pixelCharacterService.getPixelCharacter(1L, 3L)).thenReturn(response());

        mockMvc.perform(get("/api/pixel-characters/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.crtJobSttsCd").value("CHRC0003"))
                .andExpect(jsonPath("$.data.inqryRcptYn").value(false))
                .andExpect(jsonPath("$.data.heldItems[0].equipYn").value(true));
    }

    @Test
    @WithMockLoginMember
    void 남의_반려동물_캐릭터는_403_PET_ACCESS_DENIED() throws Exception {
        when(pixelCharacterService.getPixelCharacter(1L, 9L)).thenThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED));

        mockMvc.perform(get("/api/pixel-characters/9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_ACCESS_DENIED"));
    }

    @Test
    @WithMockLoginMember
    void 착용_상태를_저장한다() throws Exception {
        when(pixelCharacterService.saveEquippedItems(eq(1L), eq(3L), any())).thenReturn(response());

        mockMvc.perform(put("/api/pixel-characters/3/equipped-items")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hldItemSns\":[100]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pixelCharSn").value(3));
        verify(pixelCharacterService).saveEquippedItems(1L, 3L, new EquippedItemSaveRequest(List.of(100L)));
    }

    @Test
    @WithMockLoginMember
    void 착용_목록이_없으면_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(put("/api/pixel-characters/3/equipped-items")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 같은_부위_중복은_400_ITEM_SLOT_DUPLICATED() throws Exception {
        when(pixelCharacterService.saveEquippedItems(eq(1L), eq(3L), any()))
                .thenThrow(new CustomException(ErrorCode.ITEM_SLOT_DUPLICATED));

        mockMvc.perform(put("/api/pixel-characters/3/equipped-items")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hldItemSns\":[100,101]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_SLOT_DUPLICATED"));
    }

    @Test
    @WithMockLoginMember
    void 상점_목록에_보유_포인트와_보유중_표시가_있다() throws Exception {
        ShopItemSummary item = new ShopItemSummary();
        item.setItemSn(1L);
        item.setItemNm("밀짚모자");
        item.setItemRegnCd("MYPC0001");
        item.setNeedPnt(600L);
        item.setHldYn(true);
        when(itemService.searchShopItems(1L, 3L, "MYPC0001")).thenReturn(new ShopItemListResponse(8450L, List.of(item)));

        mockMvc.perform(get("/api/pixel-characters/3/shop-items").param("itemRegnCd", "MYPC0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hldPnt").value(8450))
                .andExpect(jsonPath("$.data.items[0].needPnt").value(600))
                .andExpect(jsonPath("$.data.items[0].hldYn").value(true));
    }

    @Test
    @WithMockLoginMember
    void 공동양육자는_상점이_403_PET_PRIMARY_ONLY() throws Exception {
        when(itemService.searchShopItems(1L, 3L, null)).thenThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY));

        mockMvc.perform(get("/api/pixel-characters/3/shop-items"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_PRIMARY_ONLY"));
    }

    @Test
    @WithMockLoginMember
    void 아이템을_구매하면_201과_보관함을_돌려준다() throws Exception {
        when(itemService.purchase(eq(1L), eq(3L), any())).thenReturn(response());

        mockMvc.perform(post("/api/pixel-characters/3/held-items")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idmptDmndId\":\"req-1\",\"itemSns\":[1,2]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.pixelCharSn").value(3));
        verify(itemService).purchase(1L, 3L, new ItemPurchaseRequest("req-1", List.of(1L, 2L)));
    }

    @Test
    @WithMockLoginMember
    void 담은_아이템이_없으면_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(post("/api/pixel-characters/3/held-items")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idmptDmndId\":\"req-1\",\"itemSns\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 공동양육자는_구매가_403_PET_PRIMARY_ONLY() throws Exception {
        when(itemService.purchase(eq(1L), eq(3L), any())).thenThrow(new CustomException(ErrorCode.PET_PRIMARY_ONLY));

        mockMvc.perform(post("/api/pixel-characters/3/held-items")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idmptDmndId\":\"req-1\",\"itemSns\":[1]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_PRIMARY_ONLY"));
    }

    @Test
    @WithMockLoginMember
    void 포인트가_부족하면_400_POINT_NOT_ENOUGH() throws Exception {
        when(itemService.purchase(eq(1L), eq(3L), any())).thenThrow(new CustomException(ErrorCode.POINT_NOT_ENOUGH));

        mockMvc.perform(post("/api/pixel-characters/3/held-items")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idmptDmndId\":\"req-1\",\"itemSns\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POINT_NOT_ENOUGH"));
    }
}
