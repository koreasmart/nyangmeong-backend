package com.nyangmeong.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.member.dto.MemberCreateRequest;
import com.nyangmeong.member.dto.MemberJoinResult;
import com.nyangmeong.member.dto.MemberNicknameAvailabilityResponse;
import com.nyangmeong.member.dto.MemberNicknameUpdateRequest;
import com.nyangmeong.member.dto.MemberPhoneAvailabilityResponse;
import com.nyangmeong.member.dto.MemberResponse;
import com.nyangmeong.member.service.MemberJoinService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.NicknameSetupTokenManager;

import jakarta.servlet.http.Cookie;

/**
 * [검사 - 회원 API]
 * - 가입은 로그인 쿠키 없이 201 + 닉네임 설정 쿠키만 / 입력 오류는 서비스까지 가지 않는다
 * - 닉네임 설정은 로그인 회원 또는 닉네임 설정 쿠키만 / 둘 다 없으면 401
 * - 쿠키는 실제 NicknameSetupTokenManager 로 발급·검증한다
 */
@WebMvcTest(controllers = MemberController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, NicknameSetupTokenManager.class,
        FieldCryptoService.class, CryptoProperties.class})
class MemberControllerTest {

    private static final String JOIN_BODY = """
            {"joinPathCd":"USRC0006","emlCertSn":3,"certNo":"123456","mblTelno":"010-1234-5678",
             "pswd":"pass1234","pswdCnfrm":"pass1234",
             "trmsAgres":[{"trmsSn":1,"agreYn":true},{"trmsSn":4,"agreYn":false}],"idmptDmndId":"req-1"}
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    NicknameSetupTokenManager nicknameSetupTokenManager;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    MemberJoinService memberJoinService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private final MemberResponse memberResponse = new MemberResponse("newbie@example.com", "냥멍12345678",
            "01012345678", null, "USRC0006", false, List.of());

    private MockHttpServletRequestBuilder join(String body) {
        return post("/api/members")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private MockHttpServletRequestBuilder nickname(String body) {
        return patch("/api/members/me/nickname")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private Cookie setupCookie(long mbrSn) {
        return new Cookie(NicknameSetupTokenManager.COOKIE_NAME, nicknameSetupTokenManager.issue(mbrSn).getValue());
    }

    @Test
    void 가입은_201과_닉네임_설정_쿠키만_준다() throws Exception {
        when(memberJoinService.create(any(MemberCreateRequest.class)))
                .thenReturn(new MemberJoinResult(11L, memberResponse));

        mockMvc.perform(join(JOIN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.mbrNcnm").value("냥멍12345678"))
                .andExpect(jsonPath("$.data.ncnmStngCmptnYn").value(false))
                .andExpect(header().stringValues("Set-Cookie", Matchers.contains(
                        Matchers.allOf(Matchers.startsWith("ncnm_setup="), Matchers.containsString("HttpOnly"),
                                Matchers.containsString("Path=/api/members")))))
                .andExpect(header().string("Set-Cookie", Matchers.not(Matchers.containsString("access_token"))));
    }

    @Test
    void 요청_헤더가_없으면_가입은_403이다() throws Exception {
        mockMvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON).content(JOIN_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void 비밀번호_형식이_틀리면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(join(JOIN_BODY.replace("pass1234", "password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[?(@.field=='pswd')].message")
                        .value("비밀번호는 8자 이상, 영문과 숫자를 함께 입력해 주세요."));

        verify(memberJoinService, never()).create(any());
    }

    @Test
    void 약관_동의_내용이_비어_있으면_400이다() throws Exception {
        mockMvc.perform(join("""
                {"joinPathCd":"USRC0006","emlCertSn":3,"certNo":"123456","mblTelno":"01012345678",
                 "pswd":"pass1234","pswdCnfrm":"pass1234","trmsAgres":[]}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("trmsAgres"));

        verify(memberJoinService, never()).create(any());
    }

    @Test
    void 번호가_중복이면_409이다() throws Exception {
        when(memberJoinService.create(any(MemberCreateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.PHONE_ALREADY_USED));

        mockMvc.perform(join(JOIN_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_ALREADY_USED"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void 번호_중복확인은_로그인_없이_정규화한_번호와_가능_여부를_준다() throws Exception {
        when(memberService.getPhoneAvailability(eq("010-1234-5678"), isNull()))
                .thenReturn(new MemberPhoneAvailabilityResponse("01012345678", true));

        mockMvc.perform(get("/api/members/phone-availability").param("mblTelno", "010-1234-5678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.normalizedMblTelno").value("01012345678"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 로그인_회원이_번호를_확인하면_본인_번호로_판정한다() throws Exception {
        when(memberService.getPhoneAvailability("01012345678", 5L))
                .thenReturn(new MemberPhoneAvailabilityResponse("01012345678", true));

        mockMvc.perform(get("/api/members/phone-availability").param("mblTelno", "01012345678"))
                .andExpect(status().isOk());

        verify(memberService).getPhoneAvailability("01012345678", 5L);
    }

    @Test
    void 번호가_비어_있으면_400이다() throws Exception {
        mockMvc.perform(get("/api/members/phone-availability").param("mblTelno", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message").value("휴대전화번호를 입력해 주세요."));
    }

    @Test
    void 닉네임_중복확인은_로그인_없이_가능_여부를_준다() throws Exception {
        when(memberService.getNicknameAvailability("초코맘", null))
                .thenReturn(new MemberNicknameAvailabilityResponse(true));

        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "초코맘"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void 닉네임_설정_쿠키가_있으면_그_회원_기준으로_중복을_판정한다() throws Exception {
        when(memberService.getNicknameAvailability("초코맘", 11L))
                .thenReturn(new MemberNicknameAvailabilityResponse(true));

        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "초코맘").cookie(setupCookie(11L)))
                .andExpect(status().isOk());

        verify(memberService).getNicknameAvailability("초코맘", 11L);
    }

    @Test
    void 닉네임이_열한_글자면_400이고_문구는_화면설계서와_같다() throws Exception {
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "가나다라마바사아자차카"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message").value("닉네임은 최대 10글자까지 입력할 수 있습니다."));
    }

    @Test
    void 닉네임에_특수문자와_공백이_있으면_400이다() throws Exception {
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "초코!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message").value("닉네임은 한글, 영문, 숫자만 사용할 수 있습니다."));
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", "초 코"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 닉네임이_비어_있으면_400이다() throws Exception {
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].message").value("닉네임을 입력해 주세요."));
        mockMvc.perform(get("/api/members/nickname-availability").param("mbrNcnm", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[*].message", Matchers.hasItem("닉네임을 입력해 주세요.")));
    }

    @Test
    void 로그인도_쿠키도_없으면_닉네임_설정은_401이다() throws Exception {
        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(memberService, never()).updateNickname(any(), any());
    }

    @Test
    void 닉네임_설정_쿠키가_있으면_그_회원의_닉네임을_저장한다() throws Exception {
        when(memberService.updateNickname(eq(11L), any(MemberNicknameUpdateRequest.class)))
                .thenReturn(new MemberResponse("newbie@example.com", "초코맘", "01012345678", null, "USRC0006", true,
                        List.of()));

        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}").cookie(setupCookie(11L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mbrNcnm").value("초코맘"))
                .andExpect(jsonPath("$.data.ncnmStngCmptnYn").value(true))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void 위조한_쿠키로는_닉네임을_저장할_수_없다() throws Exception {
        Cookie forged = new Cookie(NicknameSetupTokenManager.COOKIE_NAME, "99.9999999999." + "0".repeat(64));

        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}").cookie(forged))
                .andExpect(status().isUnauthorized());

        verify(memberService, never()).updateNickname(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 5L)
    void 로그인_회원은_쿠키_없이_닉네임을_저장한다() throws Exception {
        when(memberService.updateNickname(eq(5L), any(MemberNicknameUpdateRequest.class))).thenReturn(memberResponse);

        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}"))
                .andExpect(status().isOk());

        verify(memberService).updateNickname(eq(5L), any(MemberNicknameUpdateRequest.class));
    }

    @Test
    void 닉네임이_중복이면_409이다() throws Exception {
        when(memberService.updateNickname(eq(11L), any(MemberNicknameUpdateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.NICKNAME_ALREADY_USED));

        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘\"}").cookie(setupCookie(11L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요."));
    }

    @Test
    void 닉네임_형식이_틀리면_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(nickname("{\"mbrNcnm\":\"초코맘!!\"}").cookie(setupCookie(11L)))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).updateNickname(any(), any());
    }
}
